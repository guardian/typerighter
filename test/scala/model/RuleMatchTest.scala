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
    mappedMatch.fromPos shouldBe 15
    mappedMatch.toPos shouldBe 20
  }

  it should "account for a range ignored within the given range" in {
    val ruleMatch = getRuleMatch(10, 15)
    val ignoredRange = List(TextRange(10, 15))
    val mappedMatch = ruleMatch.mapThroughIgnoredRanges(ignoredRange)
    mappedMatch.fromPos shouldBe 15
    mappedMatch.toPos shouldBe 20
  }

  it should "account for a range ignored within the given range, and extending beyond it" in {
    val ruleMatch = getRuleMatch(8, 12)
    val ignoredRange = List(TextRange(8, 15))
    val mappedMatch = ruleMatch.mapThroughIgnoredRanges(ignoredRange)
    mappedMatch.fromPos shouldBe 15
    mappedMatch.toPos shouldBe 19
  }

  it should "account for a range ignored partially within the given range – left hand side" in {
    val ruleMatch = getRuleMatch(10, 15)
    val ignoredRange = List(TextRange(5, 12))
    val mappedMatch = ruleMatch.mapThroughIgnoredRanges(ignoredRange)
    mappedMatch.fromPos shouldBe 17
    mappedMatch.toPos shouldBe 22
  }

  it should "account for a range ignored partially the given range – right hand side" in {
    val ruleMatch = getRuleMatch(10, 15)
    val ignoredRange = List(TextRange(13, 20))
    val mappedMatch = ruleMatch.mapThroughIgnoredRanges(ignoredRange)
    mappedMatch.fromPos shouldBe 10
    mappedMatch.toPos shouldBe 22
  }
}
