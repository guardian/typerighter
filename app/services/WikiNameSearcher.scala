package services

import java.net.URLEncoder

import play.api.libs.json.{Json, Reads, Writes}
import play.api.libs.ws._

import scala.concurrent.{ExecutionContext, Future}

case class SearchResults(
                          hits: SearchHits,
                          took: Int
                        )

object SearchResults {
  implicit val writes: Writes[SearchResults] = Json.writes[SearchResults]
  implicit val reads: Reads[SearchResults] = Json.reads[SearchResults]
}

case class SearchHits(max_score: Double, hits: List[SearchHit])

object SearchHits {
  implicit val writes: Writes[SearchHits] = Json.writes[SearchHits]
  implicit val reads: Reads[SearchHits] = Json.reads[SearchHits]
}

case class SearchHit(_source: SearchHitSource, _score: Double)

object SearchHit {
  implicit val writes: Writes[SearchHit] = Json.writes[SearchHit]
  implicit val reads: Reads[SearchHit] = Json.reads[SearchHit]
}


case class SearchHitSource(title: List[String], `abstract`: List[String], url: List[String])

object SearchHitSource {
  implicit val writes: Writes[SearchHitSource] = Json.writes[SearchHitSource]
  implicit val reads: Reads[SearchHitSource] = Json.reads[SearchHitSource]
}

class WikiNameSearcher(ws: WSClient)(implicit ec: ExecutionContext) {
  def fetchWikiMatchesForName(name: String): Future[Option[SearchResults]] = {
    val request = Json.obj("query" -> Json.obj(
      "multi_match" -> Json.obj(
        "query" -> name,
        "fields" -> Json.arr("title"),
        "fuzziness" -> "AUTO"
      )
    ))
    ws.url("http://localhost:9200/wiki/_doc/_search").post(request).map { response =>
      Json.fromJson[SearchResults](response.json).asOpt
    }
  }
}
