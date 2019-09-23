package services

import java.net.URLEncoder

import play.api.libs.json.{JsSuccess, Json, Reads, Writes}
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

case class SearchHits(max_score: Option[Double], hits: List[SearchHit])

object SearchHits {
  implicit val writes: Writes[SearchHits] = Json.writes[SearchHits]
  implicit val reads: Reads[SearchHits] = Json.reads[SearchHits]
}

case class SearchHit(_source: SearchHitSource, _score: Double)

object SearchHit {
  implicit val writes: Writes[SearchHit] = Json.writes[SearchHit]
  implicit val reads: Reads[SearchHit] = Json.reads[SearchHit]
}


case class SearchHitSource(page_title: String)

object SearchHitSource {
  implicit val writes: Writes[SearchHitSource] = Json.writes[SearchHitSource]
  implicit val reads: Reads[SearchHitSource] = Json.reads[SearchHitSource]
}


case class WikiNameResult(totalHits: Int, results: List[WikiNameSearchResult])
case class WikiNameSearchResult(name: String, title: String, score: Double)

class WikiNameSearcher(ws: WSClient)(implicit ec: ExecutionContext) {
  def fetchWikiMatchesForName(name: String): Future[WikiNameResult] = {
    // Page titles contain underscores, not spaces.
    val normalisedName = name.replace(" ", "_")
    val request = Json.obj("query" -> Json.obj(
      "multi_match" -> Json.obj(
        "query" -> normalisedName,
        "fields" -> Json.arr("message"),
        "fuzziness" -> "AUTO",
        "prefix_length" -> 2
      )
    ))

    ws.url("http://localhost:9200/wiki/_doc/_search").post(request).flatMap { response =>
      val maybeJsonResponse = Json.fromJson[SearchResults](response.json).asOpt
      val titlesAndScores = maybeJsonResponse.map { results =>
        results.hits.hits.map { hit => (hit._source.page_title.mkString, hit._score) }
      }.getOrElse(List.empty)
      val searchResults = titlesAndScores.map {
        case (title, score) =>
          checkIsPerson(title).map {
            case true => {
              val denormalisedTitle = title.replace("_", " ")
              Some(WikiNameSearchResult(denormalisedTitle, title, score))
            }
            case false => None
          }
      }
      Future.sequence(searchResults).map(results => (titlesAndScores.length, results.flatten))
    }.map {
      case (totalHits, matches) => WikiNameResult(totalHits, matches)
    }
  }

  /**
    * Does the given resourceName represent a person?
    *
    * NB: We ask DBpedia for the answer to this question. This also
    * has the valuable side-effect of stripping redirects from results.
    */
  def checkIsPerson(resourceName: String): Future[Boolean] = {
    val query =
      s"""
        | SELECT count(*) as ?count
        | WHERE {
        |   { <http://dbpedia.org/resource/${resourceName}> rdf:type <http://dbpedia.org/ontology/Person> }
        | }
      """.stripMargin
    ws.url(getSparqlUrl(query)).get().map { response =>
      val maybeCount = (response.json \ "results" \ "bindings" \ 0 \ "count" \ "value").validate[String] match {
        case JsSuccess(value, _) => Some(value.toInt)
        case _ => None
      }
      maybeCount.getOrElse(0) > 0
    }
  }

  def getSparqlUrl(query: String) = s"http://dbpedia.org/sparql?default-graph-uri=http%3A%2F%2Fdbpedia.org&query=${query}&format=application%2Fsparql-results%2Bjson&CXML_redir_for_subjs=121&CXML_redir_for_hrefs=&timeout=30000&debug=on&run=+Run+Query+"
}
