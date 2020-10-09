package model

import scala.collection.JavaConverters._
import net.logstash.logback.marker.Markers
import play.api.libs.json.{Json, Reads}

case class Check(
  documentId: Option[String],
  requestId: String,
  categoryIds: Option[Set[String]],
  blocks: List[TextBlock],
  rangesToIgnore: Option[List[TextRange]] = None
) {
  def toMarker = Markers.appendEntries(Map(
    "requestId" -> this.requestId,
    "documentId" -> this.documentId,
    "blocks" -> this.blocks.map(_.id).mkString(", "),
    "categoryIds" -> this.categoryIds.mkString(", ")
  ).asJava)
}


object Check {
  implicit val reads: Reads[Check] = Json.reads[Check]
}
