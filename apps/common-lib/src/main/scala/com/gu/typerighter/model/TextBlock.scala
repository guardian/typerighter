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
    to: Int
)

object TextBlock {
  implicit val reads: Reads[TextBlock] = Json.reads[TextBlock]
  implicit val writes: Writes[TextBlock] = Json.writes[TextBlock]

  private val htmlCleaner = new HtmlCleaner()
  private val cleanerProps = htmlCleaner.getProperties
  private val serializer = new SimpleXmlSerializer(cleanerProps)

  def fromCAPIBlock(block: Block): Seq[TextBlock] =
    fromHtml(block.bodyHtml)

  private val validBlockTagNames = List("h2", "p")
  def fromHtml(htmlFragment: String): List[TextBlock] = {
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
