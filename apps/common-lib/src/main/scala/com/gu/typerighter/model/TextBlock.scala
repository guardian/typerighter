package com.gu.typerighter.model

import play.api.libs.json.{Json, Reads, Writes}
import com.gu.contentapi.client.model.v1.Block
import org.htmlcleaner.{HtmlCleaner, SimpleXmlSerializer}

import java.io.StringWriter
import scala.xml._

/** A block of text to match against.
  */
case class TextBlock(
    id: String,
    text: String,
    from: Int,
    to: Int,
    skipRanges: Option[List[TextRange]] = None
) {

  /** Remove the given ranges from the block text, adjusting the block range accordingly.
    */
  def removeSkippedRanges(): TextBlock = {
    val (newBlock, _) = skipRanges
      .getOrElse(Nil)
      .foldLeft((this, List.empty[TextRange]))((acc, range) =>
        acc match {
          case (block, rangesAlreadyApplied) =>
            val mappedRange =
              rangesAlreadyApplied.foldRight(range)((acc, range) => range.mapRemovedRange(acc))
            val snipFrom = mappedRange.from - block.from
            val snipTo = snipFrom + (mappedRange.to - mappedRange.from)
            val snipRange = TextRange(Math.max(snipFrom, 0), Math.min(block.to, snipTo + 1))

            val newText =
              block.text.slice(0, snipRange.from) + block.text.slice(snipRange.to, block.text.size)
            val newBlock =
              block.copy(text = newText, to = block.from + newText.length, skipRanges = None)

            (newBlock, rangesAlreadyApplied :+ mappedRange)
        }
      )

    newBlock
  }
}

object TextBlock {
  implicit val reads: Reads[TextBlock] = Json.reads[TextBlock]
  implicit val writes: Writes[TextBlock] = Json.writes[TextBlock]

  private val htmlCleaner = new HtmlCleaner()
  private val cleanerProps = htmlCleaner.getProperties
  private val serializer = new SimpleXmlSerializer(cleanerProps)

  def fromCAPIBlock(block: Block): Seq[TextBlock] =
    fromHtml(block.bodyHtml)

  private val validBlockTagNames = List("h2", "p")
  def fromHtml(htmlFragment: String): Seq[TextBlock] = {
    val writer = new StringWriter()
    htmlCleaner
      .clean(htmlFragment)
      .findElementByName("body", false)
      .serialize(serializer, writer)

    val cleanedFragment = writer.toString
    val xml = XML.loadString(cleanedFragment)

    val (blocks, _) = xml.head.child.zipWithIndex.foldLeft((List.empty[TextBlock], 0)) {
      case ((curBlocks, curPos), (node, index)) =>
        if (validBlockTagNames.contains(node.label)) {
          val endPos = curPos + node.text.length
          val block = TextBlock(
            s"elem-${index}",
            node.text,
            curPos,
            endPos
          )
          (curBlocks :+ block, endPos + 1)
        } else (curBlocks, curPos)
    }

    blocks
  }
}
