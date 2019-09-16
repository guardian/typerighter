package model

import play.api.libs.json.{Json, Reads}

case class Check(
                  validationSetId: String,
                  categoryIds: Option[List[String]],
                  blocks: List[Block]
                )

object Check {
  implicit val reads: Reads[Check] = Json.reads[Check]
}
