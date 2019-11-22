package services

import model.{RegexRule, RuleMatch}
import utils.Matcher

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

class RegexMatcher(category: String, rules: List[RegexRule]) extends Matcher {
  def getId() = "regex-validator"

  override def check(request: MatcherRequest): Future[List[RuleMatch]] = {
    Future.successful(rules.flatMap { checkRule(request, _) })
  }

  override def getRules(): List[RegexRule] = rules

  override def getCategory(): String = category

  private def checkRule(request: MatcherRequest, rule: RegexRule): List[RuleMatch] = {
    request.blocks.flatMap { block =>
      val iterator = rule.regex.findAllMatchIn(block.text)
      val matches = new ListBuffer[RuleMatch]
      while (iterator.hasNext) {
        val currentMatch = iterator.next
        val ruleMatch = RuleMatch(
          rule = rule,
          fromPos = currentMatch.start + block.from,
          toPos = currentMatch.end + block.from,
          message = rule.description,
          shortMessage = Some(rule.description),
          suggestions = rule.suggestions
        )
        matches.append(ruleMatch)
      }
      matches.toList
    }
  }
}