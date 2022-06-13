package matchers

import model.{BaseRule, Category, HunspellRule, RuleMatch, TextBlock, TextSuggestion}
import services.{MatcherRequest, Tokenizer}
import utils.{Matcher, Text}
import dk.dren.hunspell.Hunspell

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.JavaConverters._
import nlp.EntityFinder
import nlp.NameEntity
import nlp.DistanceHelpers

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
  * A matcher for corpus-based
  */
class DictionaryMatcher(category: Category, pathToDictionary: String, names: Set[String] = Set.empty) extends Matcher {
  val tokenizer = new Tokenizer()
  val entityFinder = new EntityFinder()
  val hunspell: Hunspell = Hunspell.getInstance()
  val dictionary: Hunspell#Dictionary = hunspell.getDictionary(pathToDictionary)

  def getType() = "hunspell"

  override def check(request: MatcherRequest)(implicit ec: ExecutionContext): Future[List[RuleMatch]] = {
    Future.successful(request.blocks.flatMap(checkText))
  }

  override def getRules(): List[BaseRule] = List.empty // @todo -- placeholder

  override def getCategories(): Set[Category] = Set(category)

  private def checkText(block: TextBlock): List[RuleMatch] = {
    val words = tokenizer.tokenize(block.text)
    val nameEntities = entityFinder.findNames((block.text))
    val nameMatches = getMatchesForNameEntities(nameEntities, names, block)

    val hunspellMatches = words.flatMap {
      case wordToken@(word, from, to) if (shouldSpellcheckWord(wordToken, nameEntities) && dictionary.misspelled(word)) =>
        val suggestions = dictionary.suggest(word).asScala.toList.map(TextSuggestion(_))
        Some(getRuleMatch(word, from, to, suggestions, block))
      case _ => None
    }

    (nameMatches ++ hunspellMatches).sortBy(_.fromPos)
  }

  def getRuleMatch(word: String, from: Int, to: Int, suggestions: List[TextSuggestion], block: TextBlock): RuleMatch = {
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
      matcherType = this.getType()
    )
  }

  // https://html.spec.whatwg.org/multipage/input.html#valid-e-mail-address
  private val emailRegex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""
  private val twitterHandleRegex = """@.*"""

  private val doNotCheckList = List(emailRegex, twitterHandleRegex)

  private def shouldSpellcheckWord(word: (String, Int, Int), names: List[NameEntity]): Boolean =
    wordIsNotAName(word, names) &&
    !doNotCheckList.exists(word._1.matches)

  private def wordIsNotAName(word: (String, Int, Int), names: List[NameEntity]): Boolean =
    names.forall { name =>
      val nameLen = name.to - name.from
      name.from > word._3 ||
      name.from < word._2 && name.from + nameLen < word._2
    }

  private def getMatchesForNameEntities(nameEntities: List[NameEntity], names: Set[String], block: TextBlock): List[RuleMatch] =
    nameEntities.foldLeft(List.empty[RuleMatch])((matches, nameEntity) => {
      if (names.contains(nameEntity.text)) {
        matches :+ getRuleMatch(nameEntity.text, nameEntity.from, nameEntity.to, Nil, block)
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

        matches :+ getRuleMatch(nameEntity.text, nameEntity.from, nameEntity.to, suggestions, block)
      }
    })
}
