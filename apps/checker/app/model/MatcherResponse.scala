package model

import com.gu.typerighter.model.{RuleMatch, TextBlock}
import play.api.libs.json._

case class MatcherResponse(blocks: List[TextBlock], categoryIds: Set[String], matches: List[RuleMatch]) {
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
