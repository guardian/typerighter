package matchers

import model.{RegexRule, RuleMatch, Category}
import services.MatcherRequest
import utils.{Matcher, RuleMatchHelpers}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

/**
  * A Matcher for rules based on regular expressions.
  */
class RegexMatcher(category: Category, rules: List[RegexRule]) extends Matcher {
  def getId() = "regex-validator"

  override def check(request: MatcherRequest)(implicit ec: ExecutionContext): Future[List[RuleMatch]] = {
    Future {
      rules.foldLeft(List.empty[RuleMatch])((acc, rule) => {
        val matches = checkRule(request, rule)
        RuleMatchHelpers.removeOverlappingRules(acc, matches) ++ matches
      })
    }
  }

  override def getRules(): List[RegexRule] = rules

  override def getCategory() = category

  private def checkRule(request: MatcherRequest, rule: RegexRule): List[RuleMatch] = {
    request.blocks.flatMap { block =>
      rule.regex.findAllMatchIn(block.text).map { currentMatch =>
        rule.toMatch(
          currentMatch.start,
          currentMatch.end,
          block
        )
      }
    }
  }
}
