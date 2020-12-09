package model

import play.api.libs.json._

case class CheckError(error: String, id: Option[String] = None) {
  val `type` = "CHECK_ERROR"
}

object CheckError {
  implicit val writes = new Writes[CheckError] {
    def writes(response: CheckError) = Json.obj(
      "type" -> response.`type`,
      "id" -> response.id,
      "error" -> response.error
    )
  }
}
