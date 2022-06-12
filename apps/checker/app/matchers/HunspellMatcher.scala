package matchers

import model.{BaseRule, Category, HunspellRule, RuleMatch, TextBlock, TextSuggestion}
import services.{MatcherRequest, Tokenizer}
import utils.{Matcher, Text}
import dk.dren.hunspell.Hunspell

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.JavaConverters._

object HunspellMatcher {
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
  * A Matcher that wraps a Hunspell instance.
  */
class HunspellMatcher(category: Category, pathToDictionary: String) extends Matcher {
  val tokenizer = new Tokenizer()
  val hunspell: Hunspell = Hunspell.getInstance()
  val dictionary: Hunspell#Dictionary = hunspell.getDictionary(pathToDictionary)

  def getType() = "hunspell"

  override def check(request: MatcherRequest)(implicit ec: ExecutionContext): Future[List[RuleMatch]] = {
    Future.successful(request.blocks.flatMap(checkText))
  }

  override def getRules(): List[BaseRule] = List.empty // @todo -- placeholder

  override def getCategories(): Set[Category] = Set(category)

  def checkText(block: TextBlock): List[RuleMatch] = {
    val words = tokenizer.tokenize(block.text)

    words.flatMap {
      case (word, from, to) =>
        if (dictionary.misspelled(word)) {
          val suggestions = dictionary.suggest(word).asScala.toList.map(TextSuggestion(_))
          Some(getRuleMatch(word, from, to, suggestions, block))
        } else {
          None
        }
    }
  }

  def getRuleMatch(word: String, from: Int, to: Int, suggestions: List[TextSuggestion], block: TextBlock): RuleMatch = {
    val (precedingText, subsequentText) = Text.getSurroundingText(block.text, from, to)
    RuleMatch(
      rule = HunspellMatcher.hunspellRule(word),
      fromPos = from + block.from,
      toPos = to + block.from,
      precedingText = precedingText,
      subsequentText = subsequentText,
      matchedText = word,
      message = HunspellMatcher.hunspellMessage,
      shortMessage = Some(HunspellMatcher.hunspellMessage),
      replacement = suggestions.headOption,
      suggestions = suggestions match {
        case Nil => Nil
        case _ => suggestions.tail
      },
      matchContext = Text.getMatchTextSnippet(precedingText, word, subsequentText),
      matcherType = this.getType()
    )
  }
}
