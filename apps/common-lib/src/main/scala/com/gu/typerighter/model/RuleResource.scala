package com.gu.typerighter.model

import play.api.libs.json.{Json, Reads, Writes}

case class RuleResource(rules: List[BaseRule], ltDefaultRuleIds: List[String])

object RuleResource {
  implicit val writes: Writes[RuleResource] = Json.writes[RuleResource]
  implicit val reads: Reads[RuleResource] = Json.reads[RuleResource]
}
