package scala.model

import model._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import com.softwaremill.diffx.scalatest.DiffMatcher._

import utils.Text

class TextBlockTest extends AsyncFlatSpec with Matchers {
  behavior of "removeIgnoredRangesFromBlock"

  it should "remove the passed ignored range from the block text" in {
    val block = TextBlock("id", "Example [noted] text", 10, 28)
    val ignoredRanges = List(TextRange(18, 25))
    val newBlock = block.removeIgnoredRanges(ignoredRanges)
    newBlock.text shouldBe "Example text"
    newBlock.from shouldBe 10
    newBlock.to shouldBe 22
  }

  it should "remove multiple ignored ranges from the block text" in {
    val block = TextBlock("id", "Example [noted][noted] text", 10, 37)
    val ignoredRanges = List(TextRange(18, 24), TextRange(24, 31))
    val newBlock = block.removeIgnoredRanges(ignoredRanges)
    newBlock.text shouldBe "Example text"
    newBlock.from shouldBe 10
    newBlock.to shouldBe 22
  }

  it should "not care about order" in {
    val block = TextBlock("id", "Example [noted][noted] text", 10, 37)
    val ignoredRanges = List(TextRange(24, 31), TextRange(18, 24))
    val newBlock = block.removeIgnoredRanges(ignoredRanges)
    newBlock.text shouldBe "Example text"
    newBlock.from shouldBe 10
    newBlock.to shouldBe 22
  }
}
