package model

import play.api.libs.json._

case class ValidatorResponse(blocks: List[TextBlock], categoryIds: List[String], matches: List[RuleMatch]) {
  val `type` = "VALIDATOR_RESPONSE"
}

object ValidatorResponse {
  implicit val writes = new Writes[ValidatorResponse] {
    def writes(response: ValidatorResponse) = Json.obj(
      "type" -> response.`type`,
      "categoryIds" -> response.categoryIds,
      "blocks" -> response.blocks,
      "matches" -> response.matches
    )
  }
}
