package model

import play.api.libs.json.{Json, Reads, Writes}

case class Block(id: String, text: String, from: Int, to: Int)

object Block {
  implicit val reads: Reads[Block] = Json.reads[Block]
  implicit val writes: Writes[Block] = Json.writes[Block]
}