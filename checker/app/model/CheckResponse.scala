package model

import play.api.libs.json._

case class CheckResponse(blocks: List[TextBlock], categoryIds: Set[String], matches: List[RuleMatch]) {
  val `type` = "CHECK_RESPONSE"
}

object CheckResponse {
  implicit val writes = new Writes[CheckResponse] {
    def writes(response: CheckResponse) = Json.obj(
      "type" -> response.`type`,
      "categoryIds" -> response.categoryIds,
      "blocks" -> response.blocks,
      "matches" -> response.matches
    )
  }

  def fromCheckResult(result: CheckResult) =
    CheckResponse(result.blocks, result.categoryIds, result.matches)
}
