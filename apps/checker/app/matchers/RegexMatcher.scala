package matchers

import com.gu.typerighter.model.{RegexRule, RuleMatch, TextBlock, TextRange}
import services.MatcherRequest
import utils.{Matcher, MatcherCompanion, RuleMatchHelpers}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import services.SentenceHelpers
import services.WordInSentence

object RegexMatcher extends MatcherCompanion {
  def getType() = "regex"
}

/** A Matcher for rules based on regular expressions.
  */
class RegexMatcher(rules: List[RegexRule]) extends Matcher {
  val sentenceHelper = new SentenceHelpers()

  def getType() = RegexMatcher.getType()

  override def check(
      request: MatcherRequest
  )(implicit ec: ExecutionContext): Future[List[RuleMatch]] = {
    Future {
      // We compute these per-block and pass them through to avoid duplicating work
      val blocksAndSentenceStarts = request.blocks.map { block =>
        (block, sentenceHelper.getFirstWordsInSentences(block.text))
      }
      rules.foldLeft(List.empty[RuleMatch])((acc, rule) => {
        val matches = checkRule(blocksAndSentenceStarts, rule)
        RuleMatchHelpers.removeOverlappingRules(acc, matches) ++ matches
      })
    }
  }

  override def getRules(): List[RegexRule] = rules

  override def getCategories() = rules.map(_.category).toSet

  private def checkRule(
      blocksAndSentenceStarts: List[(TextBlock, List[WordInSentence])],
      rule: RegexRule
  ): List[RuleMatch] =
    blocksAndSentenceStarts.flatMap { case (block, firstWordsInSentences) =>
      rule.regex.findAllMatchIn(block.text).map { currentMatch =>
        rule.toMatch(
          currentMatch.start,
          currentMatch.end,
          block,
          doesMatchCoverSentenceStart(
            firstWordsInSentences,
            TextRange(currentMatch.start, currentMatch.end)
          )
        )
      }
    }

  private def doesMatchCoverSentenceStart(
      sentenceStarts: List[WordInSentence],
      range: TextRange
  ): Boolean = {
    sentenceStarts.exists(sentenceStart => sentenceStart.range.from == range.from)
  }
}
