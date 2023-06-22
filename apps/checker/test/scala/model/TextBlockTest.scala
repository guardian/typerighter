package scala.model

import com.gu.typerighter.model.{TextBlock, TextRange}
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

  behavior of "fromHtml"

  it should "parse the top level p and h2 tags of an article body, returning a TextBlock for each tag" in {
    val html =
      """
        |<p>Example text</p>
        |<p>More text</p>
        |<h2>A heading</h2>
        |<p>Even more text</p>
        |""".stripMargin

    val blocks = TextBlock.fromHtml(html)

    blocks shouldBe List(
      TextBlock("elem-1", "Example text", 0, 12, None),
      TextBlock("elem-3", "More text", 13, 22, None),
      TextBlock("elem-5", "A heading", 23, 32, None),
      TextBlock("elem-7", "Even more text", 33, 47, None)
    )
  }

  it should "ignore non-text elements" in {
    val html =
      """
        |<aside class="element element-pullquote element--supporting"> <blockquote> <p>asdsa</p> </blockquote> </aside>  <figure class="element element-embed" >  <iframe class="fenced" srcdoc="&lt;html&gt;&lt;head&gt;&lt;/head&gt;&lt;body&gt;&lt;div&gt;&lt;/div&gt;&lt;/body&gt;&lt;/html&gt;"></iframe> </figure>  <figure class="element element-image" data-media-id="5456411bfa06a7186f07449692eeb2de16b9aaf3"> <img src="https://s3-eu-west-1.amazonaws.com/media-origin.test.dev-guim.co.uk/5456411bfa06a7186f07449692eeb2de16b9aaf3/65_0_1624_2028/801.jpg" alt="asd" width="801" height="1000" class="gu-image" /> <figcaption> <span class="element-image__caption">Sasha Velour - The Big Reveal, Wallis Annenberg Center for the Performing Arts, Los Angeles, California, USA - 15 May 2023<br>Mandatory Credit: Photo by Rob Latour/Shutterstock for The Wallis Annenberg Center (13914242dh) Sasha Velour Sasha Velour - The Big Reveal, Wallis Annenberg Center for the Performing Arts, Los Angeles, California, USA - 15 May 2023</span> <span class="element-image__credit">Photograph: Rob Latour/Shutterstock for The Wallis Annenberg Center</span> </figcaption> </figure>
        |""".stripMargin

    val blocks = TextBlock.fromHtml(html)

    blocks shouldBe List()
  }

  it should "handle an empty string" in {
    val html = ""

    val blocks = TextBlock.fromHtml(html)

    blocks shouldBe List()
  }
}
