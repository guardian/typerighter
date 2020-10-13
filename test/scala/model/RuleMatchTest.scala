package scala.model


import model._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import com.softwaremill.diffx.scalatest.DiffMatcher._

import utils.Text

class RuleMatchTest extends AsyncFlatSpec with Matchers {
  def getRuleMatch(from: Int, to: Int) = RuleMatch(
    rule = RegexRule(
      id = "test-rule",
      description = "test-description",
      category = Category("test-category", "Test Category"),
      regex = "test"r
    ),
    fromPos = from,
    toPos = to,
    precedingText = "",
    subsequentText = "",
    matchedText = "placeholder text",
    message = "placeholder message",
    matchContext = "[placeholder text]",
    matcherType = "regex"
  )

  behavior of "mapMatchThroughSkippedRanges"

  it should "account for a range skipped before the given range" in {
    val ruleMatch = getRuleMatch(10, 15)
    val skippedRange = List(TextRange(0, 5))
    val mappedMatch = ruleMatch.mapThroughSkippedRanges(skippedRange)
    mappedMatch.fromPos shouldBe 16
    mappedMatch.toPos shouldBe 21
  }

  it should "account for a range skipped within the given range" in {
    val ruleMatch = getRuleMatch(10, 15)
    val skippedRange = List(TextRange(10, 15))
    val mappedMatch = ruleMatch.mapThroughSkippedRanges(skippedRange)
    mappedMatch.fromPos shouldBe 16
    mappedMatch.toPos shouldBe 21
  }

  it should "account for a range skipped within the given range, and extending beyond it" in {
    val ruleMatch = getRuleMatch(8, 12)
    val skippedRange = List(TextRange(8, 15))
    val mappedMatch = ruleMatch.mapThroughSkippedRanges(skippedRange)
    mappedMatch.fromPos shouldBe 16
    mappedMatch.toPos shouldBe 20
  }

  it should "account for a range skipped partially within the given range – left hand side" in {
    val ruleMatch = getRuleMatch(10, 15)
    val skippedRange = List(TextRange(5, 12))
    val mappedMatch = ruleMatch.mapThroughSkippedRanges(skippedRange)
    mappedMatch.fromPos shouldBe 18
    mappedMatch.toPos shouldBe 23
  }

  it should "account for a range skipped partially the given range – right hand side" in {
    val ruleMatch = getRuleMatch(10, 15)
    val skippedRange = List(TextRange(13, 20))
    val mappedMatch = ruleMatch.mapThroughSkippedRanges(skippedRange)
    mappedMatch.fromPos shouldBe 10
    mappedMatch.toPos shouldBe 23
  }

    it should "account for multiple ranges" in {
    // E.g. "Example [noted ]text with more [noted ]text"
    val ruleMatch = getRuleMatch(18, 22)
    val skippedRange = List(TextRange(18, 25), TextRange(40, 47))
    val mappedMatch = ruleMatch.mapThroughSkippedRanges(skippedRange)
    mappedMatch.fromPos shouldBe 26
    mappedMatch.toPos shouldBe 30
  }
}
