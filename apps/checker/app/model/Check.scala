package model

import scala.jdk.CollectionConverters._
import net.logstash.logback.marker.Markers
import play.api.libs.json.{Json, Reads, Writes}
import net.logstash.logback.marker.LogstashMarker
import com.gu.pandomainauth.model.User
import com.gu.typerighter.model.{RuleMatch, TextBlock}

object Check {
  implicit val reads: Reads[Check] = Json.reads[Check]
}

/** Everything Typerighter needs to perform a check against the given blocks.
  */
case class Check(
    documentId: Option[String],
    requestId: String,
    categoryIds: Option[Set[String]],
    blocks: List[TextBlock]
) {
  def toMarker: LogstashMarker = Markers.appendEntries(
    Map(
      "requestId" -> this.requestId,
      "documentId" -> this.documentId,
      "blocks" -> this.blocks.map(_.id).mkString(", "),
      "categoryIds" -> this.categoryIds.mkString(", ")
    ).asJava
  )

  def toMarker(user: User): LogstashMarker = {
    val checkMarkers = this.toMarker
    val userMarkers = Markers.appendEntries(Map("userEmail" -> user.email).asJava)
    checkMarkers.add(userMarkers)
    checkMarkers
  }
}

/** A single result produced by a check. Checks can produce many CheckResults.
  */
object CheckResult {
  implicit val writes: Writes[CheckResult] = Json.writes[CheckResult]
}

case class CheckResult(
    categoryIds: Set[String],
    blocks: List[TextBlock],
    matches: List[RuleMatch],
    percentageRequestComplete: Option[Float] = None
)
