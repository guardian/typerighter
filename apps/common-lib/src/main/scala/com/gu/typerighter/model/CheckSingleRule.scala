package com.gu.typerighter.model

import net.logstash.logback.marker.{LogstashMarker, Markers}
import play.api.libs.json.{Format, Json}

import scala.jdk.CollectionConverters._

object CheckSingleRule {
  implicit val format: Format[CheckSingleRule] = Json.format[CheckSingleRule]
}

/** Everything Typerighter needs to get matches for a list of documents against a single rule.
  */
case class CheckSingleRule(
    requestId: String,
    rule: CheckerRule,
    documents: List[Document]
) {
  def toMarker: LogstashMarker = Markers.appendEntries(toMarkerMap.asJava)

  def toMarker(userId: String): LogstashMarker = Markers.appendEntries(
    (toMarkerMap + ("userId" -> userId)).asJava
  )

  private def toMarkerMap = Map(
    "requestId" -> this.requestId,
    "documentIds" -> this.documents.map(_.id).mkString(", "),
    "ruleId" -> this.rule.id
  )
}

/** A single result produced by a check. Checks can produce many CheckResults.
  */
object CheckSingleRuleResult {
  implicit val format: Format[CheckSingleRuleResult] = Json.format[CheckSingleRuleResult]
}

case class CheckSingleRuleResult(
    matches: List[RuleMatch],
    percentageRequestComplete: Option[Int] = None
)
