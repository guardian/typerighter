package services

import com.gu.contentapi.client.model.v1.{SearchResponse, SectionsResponse, TagsResponse}
import com.gu.contentapi.client.{ContentApiClient, GuardianContentClient}
import play.api.libs.json.{Json, Reads, Writes}

import scala.concurrent.{ExecutionContext, Future}

object CapiSearchParams {
  implicit val writes: Writes[CapiSearchParams] = Json.writes[CapiSearchParams]
  implicit val reads: Reads[CapiSearchParams] = Json.reads[CapiSearchParams]
}

case class CapiSearchParams(query: String, tags: Option[List[String]] = None, sections: Option[List[String]] = None)

class ContentClient(client: GuardianContentClient) {
  /**
    * Search the Content API for articles with the given query parameters.
    */
  def searchContent(searchParams: CapiSearchParams)(implicit ec: ExecutionContext): Future[SearchResponse] = {
    val query = ContentApiClient.search.q(searchParams.query)
    val queryWithTags = searchParams.tags.getOrElse(List.empty).foldLeft(query) { case (q, tag) => q.tag(tag)}
    val queryWithSections = searchParams.sections.getOrElse(List.empty).foldLeft(queryWithTags) { case (q, section) => q.section(section)}
    client.getResponse(queryWithSections)
  }

  /**
    * Search the Content API for tags.
    */
  def searchTags(queryStr: String)(implicit ec: ExecutionContext): Future[TagsResponse] = {
    val query = ContentApiClient.tags.q(queryStr)
    client.getResponse(query)
  }

  /**
    * Search the Content API for sections.
    */
  def searchSections(queryStr: String)(implicit ec: ExecutionContext): Future[SectionsResponse] = {
    val query = ContentApiClient.sections.q(queryStr)
    client.getResponse(query)
  }
}