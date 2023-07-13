package com.gu.typerighter.model

import play.api.libs.json.{Json, Reads, Writes}

case class CheckerRuleResource(rules: List[CheckerRule])

object CheckerRuleResource {
  implicit val writes: Writes[CheckerRuleResource] = Json.writes[CheckerRuleResource]
  implicit val reads: Reads[CheckerRuleResource] = Json.reads[CheckerRuleResource]
}
