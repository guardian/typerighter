package model

import play.api.libs.json.{Json, Reads, Writes}

/**
  * A block of text to match against.
  */
case class TextBlock(id: String, text: String, from: Int, to: Int)

object TextBlock {
  implicit val reads: Reads[TextBlock] = Json.reads[TextBlock]
  implicit val writes: Writes[TextBlock] = Json.writes[TextBlock]
}
