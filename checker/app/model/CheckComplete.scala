package model

import play.api.libs.json.{Json, Writes}

case class CheckComplete() {
  val `type` = "CHECK_COMPLETE"
}

object CheckComplete {
  implicit val writes = new Writes[CheckComplete] {
    def writes(model: CheckComplete) = Json.obj(
      "type" -> model.`type`,
    )
  }
}
