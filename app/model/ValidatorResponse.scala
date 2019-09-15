package model

import play.api.libs.json._

case class ValidatorResponse(id: String, input: String, results: List[RuleMatch]) {
  val `type` = "VALIDATOR_RESPONSE"
}

object ValidatorResponse {
  implicit val writes = new Writes[ValidatorResponse] {
    def writes(response: ValidatorResponse) = Json.obj(
      "type" -> response.`type`,
      "id" -> response.id,
      "input" -> response.input,
      "results" -> response.results
    )
  }
}
