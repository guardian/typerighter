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

  behavior of "mapMatchThroughIgnoredRanges"

  it should "account for a range ignored before the given range" in {
    val ruleMatch = getRuleMatch(10, 15)
    val ignoredRange = List(TextRange(0, 5))
    val mappedMatch = ruleMatch.mapThroughIgnoredRanges(ignoredRange)
    mappedMatch.fromPos shouldBe 16
    mappedMatch.toPos shouldBe 21
  }

  it should "account for a range ignored within the given range" in {
    val ruleMatch = getRuleMatch(10, 15)
    val ignoredRange = List(TextRange(10, 15))
    val mappedMatch = ruleMatch.mapThroughIgnoredRanges(ignoredRange)
    mappedMatch.fromPos shouldBe 16
    mappedMatch.toPos shouldBe 21
  }

  it should "account for a range ignored within the given range, and extending beyond it" in {
    val ruleMatch = getRuleMatch(8, 12)
    val ignoredRange = List(TextRange(8, 15))
    val mappedMatch = ruleMatch.mapThroughIgnoredRanges(ignoredRange)
    mappedMatch.fromPos shouldBe 16
    mappedMatch.toPos shouldBe 20
  }

  it should "account for a range ignored partially within the given range – left hand side" in {
    val ruleMatch = getRuleMatch(10, 15)
    val ignoredRange = List(TextRange(5, 12))
    val mappedMatch = ruleMatch.mapThroughIgnoredRanges(ignoredRange)
    mappedMatch.fromPos shouldBe 18
    mappedMatch.toPos shouldBe 23
  }

  it should "account for a range ignored partially the given range – right hand side" in {
    val ruleMatch = getRuleMatch(10, 15)
    val ignoredRange = List(TextRange(13, 20))
    val mappedMatch = ruleMatch.mapThroughIgnoredRanges(ignoredRange)
    mappedMatch.fromPos shouldBe 11
    mappedMatch.toPos shouldBe 23
  }

    it should "account for multiple ranges" in {
    // E.g. "Example [noted ]text with more [noted ]text"
    val ruleMatch = getRuleMatch(18, 22)
    val ignoredRange = List(TextRange(18, 25), TextRange(40, 47))
    val mappedMatch = ruleMatch.mapThroughIgnoredRanges(ignoredRange)
    mappedMatch.fromPos shouldBe 26
    mappedMatch.toPos shouldBe 30
  }
}
