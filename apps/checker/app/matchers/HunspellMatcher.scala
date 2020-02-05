package matchers

import model.{BaseRule, Category, RuleMatch, TextSuggestion}
import services.{MatcherRequest, Tokenizer}
import utils.Matcher
import dk.dren.hunspell.Hunspell

import scala.concurrent.Future
import scala.collection.JavaConverters._

/**
  * A Matcher that wraps a Hunspell instance.
  */
class HunspellMatcher(category: String, dictionaryName: String) extends Matcher {
  val tokenizer = new Tokenizer()
  val hunspell = Hunspell.getInstance()
  val dictionary = hunspell.getDictionary(dictionaryName)
  val hunspellMessage = "This word may be misspelled"
  val hunspellRule = BaseRule(
    "hunspell-rule",
    Category("spelling", "Spelling", "orange"),
    hunspellMessage,
    Nil,
    None
  )

  def getId() = "hunspell-matcher"

  override def check(request: MatcherRequest): Future[List[RuleMatch]] = {
    Future.successful(request.blocks.flatMap(block => checkText(block.text)))
  }

  override def getRules(): List[BaseRule] = List.empty // @todo -- placeholder

  override def getCategory(): String = category

  def checkText(text: String): List[RuleMatch] = {
    val words = tokenizer.tokenize(text)
    words.flatMap {
      case (word, from, to) =>
        if (dictionary.misspelled(word)) {
          val suggestions = dictionary.suggest(word).asScala.toList.map(TextSuggestion(_))
          Some(getRuleMatch(word, from, to, suggestions))
        } else {
          None
        }
    }
  }

  def getRuleMatch(word: String, from: Int, to: Int, suggestions: List[TextSuggestion]): RuleMatch = {
    RuleMatch(
      hunspellRule,
      from,
      to,
      word,
      hunspellMessage,
      Some(hunspellMessage),
      suggestions
    )
  }
}