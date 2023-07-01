package model

import com.gu.typerighter.fixtures.RuleMatchFixtures
import com.gu.typerighter.model.{Category, ComparableRegex, RegexRule, RuleMatch, TextRange}
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class RuleMatchTest extends AsyncFlatSpec with Matchers {
  behavior of "mapMatchThroughSkippedRanges"

  it should "account for a range skipped before the given range" in {
    val ruleMatch = RuleMatchFixtures.RuleMatchFixtures.getRuleMatch(10, 15)
    val skippedRange = List(TextRange(0, 5))
    val mappedMatch = ruleMatch.mapThroughSkippedRanges(skippedRange)
    mappedMatch.fromPos shouldBe 16
    mappedMatch.toPos shouldBe 21
  }

  it should "account for a range skipped within the given range" in {
    val ruleMatch = RuleMatchFixtures.getRuleMatch(10, 15)
    val skippedRange = List(TextRange(10, 15))
    val mappedMatch = ruleMatch.mapThroughSkippedRanges(skippedRange)
    mappedMatch.fromPos shouldBe 16
    mappedMatch.toPos shouldBe 21
  }

  it should "account for a range skipped within the given range, and extending beyond it" in {
    val ruleMatch = RuleMatchFixtures.getRuleMatch(8, 12)
    val skippedRange = List(TextRange(8, 15))
    val mappedMatch = ruleMatch.mapThroughSkippedRanges(skippedRange)
    mappedMatch.fromPos shouldBe 16
    mappedMatch.toPos shouldBe 20
  }

  it should "account for a range skipped partially within the given range – left hand side" in {
    val ruleMatch = RuleMatchFixtures.getRuleMatch(10, 15)
    val skippedRange = List(TextRange(5, 12))
    val mappedMatch = ruleMatch.mapThroughSkippedRanges(skippedRange)
    mappedMatch.fromPos shouldBe 18
    mappedMatch.toPos shouldBe 23
  }

  it should "account for a range skipped partially the given range – right hand side" in {
    val ruleMatch = RuleMatchFixtures.getRuleMatch(10, 15)
    val skippedRange = List(TextRange(13, 20))
    val mappedMatch = ruleMatch.mapThroughSkippedRanges(skippedRange)
    mappedMatch.fromPos shouldBe 10
    mappedMatch.toPos shouldBe 23
  }

  it should "account for multiple ranges" in {
    // E.g. "Example [noted ]text with more [noted ]text"
    val ruleMatch = RuleMatchFixtures.getRuleMatch(18, 22)
    val skippedRange = List(TextRange(18, 25), TextRange(40, 47))
    val mappedMatch = ruleMatch.mapThroughSkippedRanges(skippedRange)
    mappedMatch.fromPos shouldBe 26
    mappedMatch.toPos shouldBe 30
  }
}
