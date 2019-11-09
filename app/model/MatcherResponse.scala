package model

import play.api.libs.json._

case class MatcherResponse(blocks: List[TextBlock], categoryIds: List[String], matches: List[RuleMatch]) {
  val `type` = "VALIDATOR_RESPONSE"
}

object MatcherResponse {
  implicit val writes = new Writes[MatcherResponse] {
    def writes(response: MatcherResponse) = Json.obj(
      "type" -> response.`type`,
      "categoryIds" -> response.categoryIds,
      "blocks" -> response.blocks,
      "matches" -> response.matches
    )
  }
}
