package services

import java.net.URLEncoder

import play.api.libs.json.{Json, Reads, Writes}
import play.api.libs.ws._

import scala.concurrent.Future

case class WikiNameSearchResults()

case class WikiQueryResponse(
                              query: WikiSearchResponse
                            )

object WikiQueryResponse {
  implicit val writes: Writes[WikiQueryResponse] = Json.writes[WikiQueryResponse]
  implicit val reads: Reads[WikiQueryResponse] = Json.reads[WikiQueryResponse]
}

case class WikiSearchResponse(
                               search: List[WikiSearchResult]
                             )

object WikiSearchResponse {
  implicit val writes: Writes[WikiSearchResponse] = Json.writes[WikiSearchResponse]
  implicit val reads: Reads[WikiSearchResponse] = Json.reads[WikiSearchResponse]
}

case class WikiSearchResult(
                             ns: Int,
                             title: String,
                             pageid: Int,
                             size: Int,
                             wordcount: Int,
                             snippet: String,
                             timestamp: String
                           )

object WikiSearchResult {
  implicit val writes: Writes[WikiSearchResult] = Json.writes[WikiSearchResult]
  implicit val reads: Reads[WikiSearchResult] = Json.reads[WikiSearchResult]
}

class WikiNameSearcher(ws: WSClient) {
  def fetchWikiMatchesForName(name: String): Future[WikiQueryResponse] = {
    ws.url(getWikiSearchUrl(name)).get().map { response =>
      Json.fromJson[WikiSearchResponse](response.json).asOpt
    }.flatten
  }

  private def getWikiSearchUrl(search: String) = s"https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=${URLEncoder.encode(search, "UTF-8")}&utf8=&format=json"

}
