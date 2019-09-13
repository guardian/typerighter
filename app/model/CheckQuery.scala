package model

import play.api.libs.json.{Json, Reads}

case class CheckQuery(id: String, text: String, categoryIds: Option[List[String]])

object CheckQuery {
  implicit val reads: Reads[CheckQuery] = Json.reads[CheckQuery]
}