package model

import play.api.libs.json.Writes
import play.api.libs.json.Reads
import play.api.libs.json.Json

case class RuleResource(regexRules: List[RegexRule], ltDefaultRuleIds: List[String])

object RuleResource {
  implicit val writes: Writes[RuleResource] = Json.writes[RuleResource]
  implicit val reads: Reads[RuleResource] = Json.reads[RuleResource]
}
