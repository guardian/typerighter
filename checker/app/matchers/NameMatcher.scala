package matchers

import model.{RegexRule, RuleMatch, Category}
import services.MatcherRequest
import utils.{Matcher, MatcherCompanion, RuleMatchHelpers}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import model.TextRange
import model.TextBlock
import model.NameRule

object NameMatcher extends MatcherCompanion {
  def getType() = "regex"
}

/**
  * A Matcher for rules based on regular expressions.
  */
class NameMatcher(rules: List[NameRule]) extends Matcher {

  def getType() = NameMatcher.getType

  override def check(request: MatcherRequest)(implicit ec: ExecutionContext): Future[List[RuleMatch]] = {
    Future {
      List()
    }
  }

  override def getRules(): List[NameRule] = rules

  override def getCategories() = rules.map(_.category).toSet
}
