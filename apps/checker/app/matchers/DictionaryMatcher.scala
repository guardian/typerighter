package matchers

import model.{BaseRule, Category, HunspellRule, RuleMatch, TextBlock, TextRange, TextSuggestion}
import services.{MatcherRequest, Tokenizer}
import utils.{Matcher, Text}

import scala.concurrent.{ExecutionContext, Future}
import nlp.{DistanceHelpers, Entity, EntityFinder, NameEntity}

object DictionaryMatcher {
  val hunspellMessage = "This word may be misspelled"
  val hunspellRule = (word: String) => HunspellRule(
    s"hunspell-rule-${word}",
    Category("spelling", "Spelling"),
    hunspellMessage,
    Nil,
    None
  )
}

/**
  * A dictionary matcher.
  *
  * Uses entity recognition to discover names and match them against known names.
  *
  * Relies on a Morfologik dictionary, via LanguageTool, for everything else.
  */
class DictionaryMatcher(category: Category, languageToolFactory: LanguageToolFactory, names: Set[String] = Set.empty) extends Matcher {
  val tokenizer = new Tokenizer()
  val entityFinder = new EntityFinder()
  val ltInstance = languageToolFactory.createSpellingInstance() match {
    case Left(error) => throw error.head
    case Right(instance) => instance
  }

  def getType() = "dictionary"

  override def check(request: MatcherRequest)(implicit ec: ExecutionContext): Future[List[RuleMatch]] = {
    val (entities, nameMatches) = request.blocks.foldLeft((List.empty[Entity], List.empty[RuleMatch])) { (acc, block) =>
      acc match {
        case (accEntities, accNameMatches) =>
          val entities = entityFinder.findNamesAndNonWordTokens(block.text, block.from)
          val nameMatches = getMatchesForNameEntities(entities, names, block)
          (accEntities ++ entities, accNameMatches ++ nameMatches)
      }
    }

    ltInstance.check(MatcherRequest(request.blocks)).map { ruleMatches =>
      // Remove any matches which intersect with entities.
      val filteredMatches = ruleMatches.filter { ruleMatch =>
        entities.forall { entity =>
          TextRange(ruleMatch.fromPos, ruleMatch.toPos).getIntersection(TextRange(entity.from, entity.to)).isEmpty
        }
      }.map { _.copy(priority = 1) }

      (nameMatches ++ filteredMatches).sortBy(_.fromPos)
    }
  }

  override def getRules(): List[BaseRule] = List.empty // @todo -- placeholder

  override def getCategories(): Set[Category] = Set(category)

  def getRuleMatch(word: String, from: Int, to: Int, suggestions: List[TextSuggestion], block: TextBlock, markAsCorrect: Boolean = false): RuleMatch = {
    val (precedingText, subsequentText) = Text.getSurroundingText(block.text, from, to)
    RuleMatch(
      rule = DictionaryMatcher.hunspellRule(word),
      fromPos = from + block.from,
      toPos = to + block.from,
      precedingText = precedingText,
      subsequentText = subsequentText,
      matchedText = word,
      message = DictionaryMatcher.hunspellMessage,
      shortMessage = Some(DictionaryMatcher.hunspellMessage),
      replacement = suggestions.headOption,
      suggestions = suggestions match {
        case Nil => Nil
        case _ => suggestions.tail
      },
      matchContext = Text.getMatchTextSnippet(precedingText, word, subsequentText),
      matcherType = this.getType(),
      markAsCorrect = markAsCorrect,
      priority = 1
    )
  }

  private def getMatchesForNameEntities(nameEntities: List[Entity], names: Set[String], block: TextBlock): List[RuleMatch] =
    nameEntities.foldLeft(List.empty[RuleMatch])((matches, nameEntity) => {
      if (names.contains(nameEntity.text)) {
        matches :+ getRuleMatch(nameEntity.text, nameEntity.from, nameEntity.to, Nil, block, true)
      } else {
        val suggestedNames = DistanceHelpers
          .findSimilarNames(nameEntity.text, names)
          .map(name => TextSuggestion(name))

        val couldBePossessiveOrContraction = suggestedNames.exists { suggestion =>
          nameEntity.text.replaceFirst("s$", "") == suggestion.text
        }

        val suggestions = if (couldBePossessiveOrContraction) {
          TextSuggestion(s"${nameEntity.text.dropRight(1)}'s") +: suggestedNames
        } else suggestedNames

        if (suggestions.length > 0) matches :+ getRuleMatch(nameEntity.text, nameEntity.from, nameEntity.to, suggestions, block)
        else matches
      }
    })
}
