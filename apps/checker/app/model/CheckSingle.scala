package model

import com.gu.typerighter.model.{CheckerRule, RuleMatch, TextBlock}
import net.logstash.logback.marker.{LogstashMarker, Markers}
import play.api.libs.json.{Json, Reads, Writes}

import scala.jdk.CollectionConverters._

object CheckSingleRule {
  implicit val reads: Reads[CheckSingleRule] = Json.reads[CheckSingleRule]
}

/** Everything Typerighter needs to a list of documents against a single rule.
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

object Document {
  implicit val reads: Reads[Document] = Json.reads[Document]
}

case class Document(id: String, blocks: List[TextBlock])

/** A single result produced by a check. Checks can produce many CheckResults.
  */
object CheckSingleRuleResult {
  implicit val writes: Writes[CheckSingleRuleResult] = Json.writes[CheckSingleRuleResult]
}

case class CheckSingleRuleResult(
    matches: List[RuleMatch],
    percentageRequestComplete: Option[Int] = None
)
