package model

import play.api.libs.json._

case class ValidatorError(error: String, id: Option[String] = None) {
  val `type` = "VALIDATOR_ERROR"
}

object ValidatorError {
  implicit val writes = new Writes[ValidatorError] {
    def writes(response: ValidatorError) = Json.obj(
      "type" -> response.`type`,
      "id" -> response.id,
      "error" -> response.error
    )
  }
}
