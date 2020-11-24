package matchers

import model.{RegexRule, RuleMatch, Category}
import services.MatcherRequest
import utils.{Matcher, MatcherCompanion, RuleMatchHelpers}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import services.SentenceHelpers
import services.WordInSentence
import model.TextRange

object RegexMatcher extends MatcherCompanion {
  def getType() = "regex"
}


/**
  * A Matcher for rules based on regular expressions.
  */
class RegexMatcher(rules: List[RegexRule]) extends Matcher {
  val sentenceHelper = new SentenceHelpers()

  def getType() = RegexMatcher.getType

  override def check(request: MatcherRequest)(implicit ec: ExecutionContext): Future[List[RuleMatch]] = {
    Future {
      rules.foldLeft(List.empty[RuleMatch])((acc, rule) => {
        val matches = checkRule(request, rule)
        RuleMatchHelpers.removeOverlappingRules(acc, matches) ++ matches
      })
    }
  }

  override def getRules(): List[RegexRule] = rules

  override def getCategories() = rules.map(_.category).toSet

  private def checkRule(request: MatcherRequest, rule: RegexRule): List[RuleMatch] = {
    request.blocks.flatMap { block =>
      val firstWordsInSentences = sentenceHelper.getFirstWordsInSentences(block.text)
      rule.regex.findAllMatchIn(block.text).map { currentMatch =>
        rule.toMatch(
          currentMatch.start,
          currentMatch.end,
          block,
          doesMatchCoverSentenceStart(firstWordsInSentences, TextRange(currentMatch.start, currentMatch.end))
        )
      }
    }
  }

  private def doesMatchCoverSentenceStart(sentenceStarts: List[WordInSentence], range: TextRange): Boolean = {
    sentenceStarts.exists(sentenceStart =>
      sentenceStart.range.from == range.from
    )
  }
}
