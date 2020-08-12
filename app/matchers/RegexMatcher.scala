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
    val matches = rules.foldLeft(List.empty[RuleMatch])((acc, rule) => {
      val matches = checkRule(request, rule)
      removeOverlappingRules(acc, matches) ++ matches
    })
    Future.successful(matches)
  }

  override def getRules(): List[RegexRule] = rules

  override def getCategory(): String = category

  private def checkRule(request: MatcherRequest, rule: RegexRule): List[RuleMatch] = {
    request.blocks.flatMap { block =>
        rule.regex.findAllMatchIn(block.text).map { currentMatch => RuleMatch.fromMatch(currentMatch.start, currentMatch.end, block, rule) }
    }
  }

  private def removeOverlappingRules(currentMatches: List[RuleMatch], incomingMatches: List[RuleMatch]): List[RuleMatch] =
    currentMatches.filter { currentMatch =>
      incomingMatches.forall { incomingMatch =>
        currentMatch.fromPos < incomingMatch.fromPos && currentMatch.toPos < incomingMatch.fromPos ||
        currentMatch.fromPos > incomingMatch.toPos
      }
    }
}
