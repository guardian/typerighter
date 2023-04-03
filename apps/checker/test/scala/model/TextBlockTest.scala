package scala.model

import com.gu.typerighter.model.{Text, TextBlock, TextRange}
import model._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

class TextBlockTest extends AsyncFlatSpec with Matchers {
  behavior of "removeSkippedRanges"

  it should "remove the passed skipped range from the block text" in {
    val skippedRanges = Some(List(TextRange(18, 25)))
    val block = TextBlock("id", "Example [noted ]text", 10, 28, skippedRanges)
    val newBlock = block.removeSkippedRanges()
    newBlock.text shouldBe "Example text"
    newBlock.from shouldBe 10
    newBlock.to shouldBe 22
  }

  it should "remove multiple adjacent skipped ranges from the block text" in {
    val skippedRanges = Some(List(TextRange(18, 25), TextRange(26, 32)))
    val block = TextBlock("id", "Example [noted][noted ]text", 10, 37, skippedRanges)
    val newBlock = block.removeSkippedRanges()

    newBlock.text shouldBe "Example text"
    newBlock.from shouldBe 10
    newBlock.to shouldBe 22
  }

  it should "remove multiple non-adjacent skipped ranges from the block text - 1" in {
    val skippedRanges = Some(List(TextRange(18, 25), TextRange(41, 48)))
    val block =
      TextBlock("id", "Example [noted ]text with more [noted ]text", 10, 52, skippedRanges)
    val newBlock = block.removeSkippedRanges()
    newBlock.text shouldBe "Example text with more text"
    newBlock.from shouldBe 10
    newBlock.to shouldBe 37
  }

  it should "remove multiple non-adjacent skipped ranges from the block text - 2" in {
    val block = TextBlock(
      "1602586488022-from:40-to:214",
      "ABCDEFGHIJ",
      40,
      47,
      Some(List(TextRange(40, 42), TextRange(44, 45), TextRange(47, 48)))
    )
    val newBlock = block.removeSkippedRanges()

    newBlock.text shouldBe "DGJ"
    newBlock.from shouldBe 40
    newBlock.to shouldBe 43
  }

  it should "not care about order" in {
    val skippedRanges = Some(List(TextRange(24, 31), TextRange(18, 24)))
    val block = TextBlock("id", "Example [noted][noted ]text", 10, 37, skippedRanges)
    val newBlock = block.removeSkippedRanges()
    newBlock.text shouldBe "Example text"
    newBlock.from shouldBe 10
    newBlock.to shouldBe 22
  }
}
