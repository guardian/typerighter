package model

import play.api.libs.json.{Json, Reads}

case class Check(
                  validationSetId: String,
                  inputs: List[CheckQuery]
                )

object Check {
  implicit val reads: Reads[Check] = Json.reads[Check]
}

case class CheckQuery(validationId: String, text: String, from: Int, to: Int, categoryIds: Option[List[String]])

object CheckQuery {
  implicit val reads: Reads[CheckQuery] = Json.reads[CheckQuery]
}