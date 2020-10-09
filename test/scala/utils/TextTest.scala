package scala.utils

import model._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import com.softwaremill.diffx.scalatest.DiffMatcher._

import utils.Text

class TextTest extends AsyncFlatSpec with Matchers {
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

  behavior of "removeIgnoredRangesFromBlock"

  it should "remove the passed ignored range from the block text" in {
    val block = TextBlock("id", "Example [noted] text", 10, 28)
    val ignoredRanges = List(TextRange(18, 25))
    val newBlock = Text.removeIgnoredRangesFromBlock(block, ignoredRanges)
    newBlock.text shouldBe "Example text"
    newBlock.from shouldBe 10
    newBlock.to shouldBe 22
  }

  it should "remove multiple ignored ranges from the block text" in {
    val block = TextBlock("id", "Example [noted][noted] text", 10, 37)
    val ignoredRanges = List(TextRange(18, 24), TextRange(24, 31))
    val newBlock = Text.removeIgnoredRangesFromBlock(block, ignoredRanges)
    newBlock.text shouldBe "Example text"
    newBlock.from shouldBe 10
    newBlock.to shouldBe 22
  }

  behavior of "mapMatchThroughIgnoredRanges"

  it should "account for a range ignored before the given range" in {
    val ruleMatch = getRuleMatch(10, 15)
    val ignoredRange = List(TextRange(0, 5))
    val mappedMatch = Text.mapMatchThroughIgnoredRanges(ruleMatch, ignoredRange)
    mappedMatch.fromPos shouldBe 15
    mappedMatch.toPos shouldBe 20
  }

  it should "account for a range ignored within the given range" in {
    val ruleMatch = getRuleMatch(10, 15)
    val ignoredRange = List(TextRange(10, 15))
    val mappedMatch = Text.mapMatchThroughIgnoredRanges(ruleMatch, ignoredRange)
    mappedMatch.fromPos shouldBe 15
    mappedMatch.toPos shouldBe 20
  }

  it should "account for a range ignored partially within the given range – left hand side" in {
    val ruleMatch = getRuleMatch(10, 15)
    val ignoredRange = List(TextRange(5, 12))
    val mappedMatch = Text.mapMatchThroughIgnoredRanges(ruleMatch, ignoredRange)
    mappedMatch.fromPos shouldBe 17
    mappedMatch.toPos shouldBe 22
  }

  it should "account for a range ignored partially the given range – right hand side" in {
    val ruleMatch = getRuleMatch(10, 15)
    val ignoredRange = List(TextRange(13, 20))
    val mappedMatch = Text.mapMatchThroughIgnoredRanges(ruleMatch, ignoredRange)
    mappedMatch.fromPos shouldBe 10
    mappedMatch.toPos shouldBe 22
  }

  behavior of "mapAddedRange"

  it should "account for a range added before the given range" in {
    val incomingRange = TextRange(10, 15)
    val addedRange = TextRange(0, 5)
    Text.mapAddedRange(addedRange, incomingRange) shouldBe TextRange(15, 20)
  }

  it should "account for a range added within the given range" in {
    val incomingRange = TextRange(10, 15)
    val addedRange = TextRange(10, 15)
    Text.mapAddedRange(addedRange, incomingRange) shouldBe TextRange(15, 20)
  }

  it should "account for a range added partially within the given range – left hand side" in {
    val incomingRange = TextRange(10, 15)
    val addedRange = TextRange(5, 12)
    Text.mapAddedRange(addedRange, incomingRange) shouldBe TextRange(17, 22)
  }

  it should "account for a range added partially the given range – right hand side" in {
    val incomingRange = TextRange(10, 15)
    val addedRange = TextRange(13, 20)
    Text.mapAddedRange(addedRange, incomingRange) shouldBe TextRange(10, 22)
  }

  behavior of "mapRemovedRange"

  it should "account for a range removed before the given range" in {
    val incomingRange = TextRange(10, 15)
    val removedRange = TextRange(0, 5)
    Text.mapRemovedRange(removedRange, incomingRange) shouldBe TextRange(5, 10)
  }

  it should "account for a range completely removed within the given range" in {
    val incomingRange = TextRange(10, 15)
    val removedRange = TextRange(10, 15)
    Text.mapRemovedRange(removedRange, incomingRange) shouldBe TextRange(10, 10)
  }

  it should "account for a range partially removed within the given range – left hand side" in {
    val incomingRange = TextRange(10, 15)
    val removedRange = TextRange(5, 12)
    Text.mapRemovedRange(removedRange, incomingRange) shouldBe TextRange(5, 8)
  }

  it should "account for a range partially within the given range – right hand side" in {
    val incomingRange = TextRange(10, 15)
    val removedRange = TextRange(13, 20)
    Text.mapRemovedRange(removedRange, incomingRange) shouldBe TextRange(10, 13)
  }

  behavior of "getIntersectionOfRanges"

  it should "return an option containing a new range representing the intersection of two ranges" in {
    val rangeA = TextRange(0, 5)
    val rangeB = TextRange(4, 6)
    Text.getIntersectionOfRanges(rangeA, rangeB) shouldBe Some(TextRange(4, 5))
  }

  it should "not return a range if there is no intersection" in {
    val rangeA = TextRange(0, 3)
    val rangeB = TextRange(4, 6)
    Text.getIntersectionOfRanges(rangeA, rangeB) shouldBe None
  }
}
