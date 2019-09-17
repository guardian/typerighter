package model

import play.api.libs.json.{Json, Reads}

case class Check(
                  requestId: String,
                  categoryIds: Option[List[String]],
                  blocks: List[TextBlock]
                )

object Check {
  implicit val reads: Reads[Check] = Json.reads[Check]
}
