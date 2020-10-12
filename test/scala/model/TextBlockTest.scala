package scala.model

import model._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import com.softwaremill.diffx.scalatest.DiffMatcher._

import utils.Text

class TextBlockTest extends AsyncFlatSpec with Matchers {
  behavior of "removeIgnoredRangesFromBlock"

  it should "remove the passed ignored range from the block text" in {
    val ignoredRanges = List(TextRange(18, 25))
    val block = TextBlock("id", "Example [noted ]text", 10, 28, ignoredRanges)
    val newBlock = block.removeIgnoredRanges
    newBlock.text shouldBe "Example text"
    newBlock.from shouldBe 10
    newBlock.to shouldBe 22
  }

  it should "remove multiple adjacent ignored ranges from the block text" in {
    val ignoredRanges = List(TextRange(18, 25), TextRange(25, 31))
    val block = TextBlock("id", "Example [noted][noted ]text", 10, 37, ignoredRanges)
    val newBlock = block.removeIgnoredRanges
    newBlock.text shouldBe "Example text"
    newBlock.from shouldBe 10
    newBlock.to shouldBe 22
  }

  it should "remove multiple non-adjacent ignored ranges from the block text" in {
    val ignoredRanges = List(TextRange(18, 25), TextRange(40, 47))
    val block = TextBlock("id", "Example [noted ]text with more [noted ]text", 10, 52, ignoredRanges)
    val newBlock = block.removeIgnoredRanges
    newBlock.text shouldBe "Example text with more text"
    newBlock.from shouldBe 10
    newBlock.to shouldBe 37
  }

  it should "not care about order" in {
    val ignoredRanges = List(TextRange(24, 31), TextRange(18, 24))
    val block = TextBlock("id", "Example [noted][noted ]text", 10, 37, ignoredRanges)
    val newBlock = block.removeIgnoredRanges
    newBlock.text shouldBe "Example text"
    newBlock.from shouldBe 10
    newBlock.to shouldBe 22
  }
}
