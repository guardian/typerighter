package model

import play.api.libs.json.{Json, Writes}

case class MatcherWorkComplete() {
  val `type` = "VALIDATOR_WORK_COMPLETE"
}

object MatcherWorkComplete {
  implicit val writes: Writes[MatcherWorkComplete] = new Writes[MatcherWorkComplete] {
    def writes(model: MatcherWorkComplete) = Json.obj(
      "type" -> model.`type`
    )
  }
}
