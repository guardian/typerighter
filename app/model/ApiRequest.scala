package model

import scala.collection.JavaConverters._
import net.logstash.logback.marker.Markers
import play.api.libs.json.{Json, Reads}

case class ApiRequest(
  documentId: Option[String],
  requestId: String,
  categoryIds: Option[List[String]],
  blocks: List[TextBlock]
) {
  def toMarker = Markers.appendEntries(Map(
    "requestId" -> this.requestId,
    "documentId" -> this.documentId,
    "blocks" -> this.blocks.map(_.id).mkString(", "),
    "categoryIds" -> this.categoryIds.mkString(", ")
  ).asJava)
}

object ApiRequest {
  implicit val reads: Reads[ApiRequest] = Json.reads[ApiRequest]
}
