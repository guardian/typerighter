package matchers

import model.{BaseRule, Rule, Category, RuleMatch, TextSuggestion}
import services.{MatcherRequest, Tokenizer}
import utils.Matcher
import dk.dren.hunspell.Hunspell

import scala.concurrent.Future
import scala.collection.JavaConverters._

object HunspellMatcher {
  val hunspellMessage = "This word may be misspelled"
  val hunspellRule: Rule = Rule(
    "hunspell-rule",
    Category("spelling", "Spelling", "orange"),
    hunspellMessage,
    Nil,
    None
  )
}

/**
  * A Matcher that wraps a Hunspell instance.
  */
class HunspellMatcher(category: String, pathToDictionary: String) extends Matcher {
  val tokenizer = new Tokenizer()
  val hunspell: Hunspell = Hunspell.getInstance()
  val dictionary: Hunspell#Dictionary = hunspell.getDictionary(pathToDictionary)

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
      HunspellMatcher.hunspellRule,
      from,
      to,
      word,
      HunspellMatcher.hunspellMessage,
      Some(HunspellMatcher.hunspellMessage),
      suggestions
    )
  }
}