package matchers

import model.{RegexRule, RuleMatch}
import services.MatcherRequest
import utils.Matcher

import scala.concurrent.Future


/**
  * A Matcher for rules based on regular expressions.
  */
class RegexMatcher(category: String, rules: List[RegexRule]) extends Matcher {
  def getId() = "regex-validator"

  override def check(request: MatcherRequest): Future[List[RuleMatch]] = {
    Future.successful(rules.flatMap { checkRule(request, _) })
  }

  override def getRules(): List[RegexRule] = rules

  override def getCategory(): String = category

  private def checkRule(request: MatcherRequest, rule: RegexRule): List[RuleMatch] = {
    request.blocks.flatMap { block =>
        rule.regex.findAllMatchIn(block.text).map { currentMatch => RuleMatch(
          rule = rule,
          fromPos = currentMatch.start + block.from,
          toPos = currentMatch.end + block.from,
          matchedText = block.text.substring(currentMatch.start, currentMatch.end),
          message = rule.description,
          shortMessage = Some(rule.description),
          suggestions = rule.suggestions,
          markAsCorrect = rule.replacement.map(_.text).getOrElse("") == block.text.substring(currentMatch.start, currentMatch.end)
        )
      }
    }
  }
}