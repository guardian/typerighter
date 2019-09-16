package model

import play.api.libs.json._

case class ValidatorResponse(blocks: List[Block], categoryId: String, results: List[RuleMatch]) {
  val `type` = "VALIDATOR_RESPONSE"
}

object ValidatorResponse {
  implicit val writes = new Writes[ValidatorResponse] {
    def writes(response: ValidatorResponse) = Json.obj(
      "type" -> response.`type`,
      "categoryId" -> response.categoryId,
      "blocks" -> response.blocks,
      "results" -> response.results
    )
  }
}
