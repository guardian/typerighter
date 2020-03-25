package services

import com.gu.contentapi.client.model.v1.{SearchResponse, SectionsResponse, TagsResponse}
import com.gu.contentapi.client.{ContentApiClient, GuardianContentClient}

import scala.concurrent.{ExecutionContext, Future}

class ContentClient(client: GuardianContentClient) {
  /**
    * Search the Content API for articles with the given query parameters.
    */
  def searchContent(queryStr: String, tags: List[String] = List.empty, sections: List[String] = List.empty)(implicit ec: ExecutionContext): Future[SearchResponse] = {
    val query = ContentApiClient.search.q(queryStr)
    val queryWithTags = tags.foldLeft(query) { case (q, tag) => q.tag(tag)}
    val queryWithSections = sections.foldLeft(queryWithTags) { case (q, section) => q.section(section)}
    client.getResponse(queryWithSections)
  }

  /**
    * Search the Content API for tags.
    */
  def searchTags(tags: String)(implicit ec: ExecutionContext): Future[TagsResponse] = {
    val query = ContentApiClient.tags.q(tags)
    client.getResponse(query)
  }

  /**
    * Search the Content API for sections.
    */
  def searchSections(sections: String)(implicit ec: ExecutionContext): Future[SectionsResponse] = {
    val query = ContentApiClient.sections.q(sections)
    client.getResponse(query)
  }
}