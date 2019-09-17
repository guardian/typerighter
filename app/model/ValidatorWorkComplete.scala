package model

import play.api.libs.json.{Json, Writes}

case class ValidatorWorkComplete() {
  val `type` = "VALIDATOR_WORK_COMPLETE"
}

object ValidatorWorkComplete {
  implicit val writes = new Writes[ValidatorWorkComplete] {
    def writes(model: ValidatorWorkComplete) = Json.obj(
      "type" -> model.`type`,
    )
  }
}
