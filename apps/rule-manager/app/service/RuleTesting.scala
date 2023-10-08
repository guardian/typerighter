package service

import akka.stream.Attributes
import akka.{Done, NotUsed}
import akka.stream.scaladsl.Source
import play.api.libs.ws.WSClient
import com.gu.typerighter.lib.{HMACClient, JsonHelpers, Loggable, ContentClient}
import com.gu.typerighter.model.{CheckSingleRule, CheckSingleRuleResult, Document}
import db.DbRuleDraft
import play.api.libs.json.{Format, JsError, JsSuccess, Json}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

case class TestRuleCapiQuery(
    queryStr: String,
    tags: Option[List[String]] = None,
    sections: Option[List[String]] = None
)

case class PaginatedCheckRuleResult(
    currentPage: Int,
    maxPages: Int,
    pageSize: Int,
    result: CheckSingleRuleResult
)

object PaginatedCheckRuleResult {
  implicit val formats: Format[PaginatedCheckRuleResult] = Json.format[PaginatedCheckRuleResult]
}

object TestRuleCapiQuery {
  implicit val format: Format[TestRuleCapiQuery] = Json.format[TestRuleCapiQuery]
}

/** Test rules against content.
  */
class RuleTesting(
    ws: WSClient,
    hmacClient: HMACClient,
    contentClient: ContentClient,
    checkerUrl: String
) extends Loggable {

  /** Test a rule against a given CAPI query. Paginates through CAPI content to return matches until
    * either:
    *   - `matchCount` is met.
    *   - `maxPageCount` is met.
    */
  def testRuleWithCapiQuery(
      rule: DbRuleDraft,
      query: TestRuleCapiQuery,
      matchCount: Int = 10,
      maxPageCount: Int = 100,
      pageSize: Int = 20
  )(implicit ec: ExecutionContext): Source[PaginatedCheckRuleResult, NotUsed] = {
    class PaginatedContentQuery(pageLimit: Int) {
      var currentPage = 1
      def hasNext = currentPage <= pageLimit
      def nextPage(): Future[(Int, List[Document])] = {
        log.info(s"Fetching content page $currentPage/$maxPageCount")
        val pageToFetch = currentPage
        currentPage = currentPage + 1
        contentClient
          .searchContent(
            query.queryStr,
            query.tags.getOrElse(List.empty),
            query.sections.getOrElse(List.empty),
            pageToFetch,
            pageSize
          )
          .map { response =>
            log.info(s"Received content page $pageToFetch/$maxPageCount")
            (pageToFetch, response.results.map(Document.fromCapiContent).toList)
          }
      }
    }

    val createResource = () => Future.successful(new PaginatedContentQuery(maxPageCount))
    val readFromResource = (paginatedQuery: PaginatedContentQuery) =>
      if (paginatedQuery.hasNext) {
        paginatedQuery.nextPage().map(Some.apply)
      } else {
        log.info(s"No more pages to fetch")
        Future.successful(None)
      }
    val closeResource = (_: PaginatedContentQuery) => Future.successful(Done)

    Source
      // Create a stream that produces pages of CAPI content
      .unfoldResourceAsync[(Int, List[Document]), PaginatedContentQuery](
        createResource,
        readFromResource,
        closeResource
      )
      // Check each page of documents, merging the resulting stream from `testRule`
      .flatMapConcat { case (page, documents) =>
        // The lazy source ensures that we only trigger `testRule` when there is demand
        Source
          .lazyFutureSource(() => testRule(rule, documents))
          .map(source => PaginatedCheckRuleResult(page, maxPageCount, pageSize, source))
      }
      // Maintain a count of the matches so we can stop once our limit is reached
      .scan((0, Option.empty[PaginatedCheckRuleResult])) { case ((count, _), result) =>
        (count + result.result.matches.size, Some(result))
      }
      .takeWhile { case (count, _) => count <= matchCount }
      .mapConcat { case (_, result) => result }
      // Adding a buffer that accepts a single element ensures that we apply backpressure
      // to the resource that's fetching our CAPI documents when we're checking downstream.
      .withAttributes(Attributes.inputBuffer(initial = 1, max = 1))
  }

  /** Test a rule against the given list of documents. Return a list of matches from the checker
    * service endpoint as a stream of json-seq records.
    */
  def testRule(rule: DbRuleDraft, documents: List[Document])(implicit ec: ExecutionContext) = {
    val liveRule = rule.toLive("placeholder")

    RuleManager.liveDbRuleToCheckerRule(liveRule).toOption match {
      case Some(rule) =>
        val path = "/checkSingle"
        val url = s"$checkerUrl$path"
        val headers = hmacClient.getHMACHeaders(path)
        val requestId = UUID.randomUUID().toString()
        val checkSingleRule = CheckSingleRule(
          requestId = requestId,
          documents = documents,
          rule = rule
        )
        val body = Json.toJson(checkSingleRule)

        log.info(
          s"Fetching results for ${documents.size} document(s) from checker service $url with body $body"
        )

        ws.url(url)
          .withHttpHeaders(headers: _*)
          .withMethod("POST")
          .withBody(body)
          .stream()
          .flatMap { response =>
            if (response.status != 200) {
              val error = s"Error sending checker request, ${response.status}: ${response.body}"
              log.error(error)
              Future.failed(new Throwable(error))
            } else {
              val responseStream = response.bodyAsSource
                .via(JsonHelpers.JsonSeqFraming)
                .mapConcat { str =>
                  Json.parse(str.utf8String).validate[CheckSingleRuleResult] match {
                    case JsSuccess(value, _) =>
                      log.info(
                        s"Received ${value.matches.length} matches from checker service${value.percentageRequestComplete
                            .map(p => s", $p% complete")
                            .getOrElse("")}"
                      )
                      Some(value)
                    case JsError(error) =>
                      log.error(s"Error parsing checker result: ${error.toString}")
                      None
                  }
                }

              Future.successful(responseStream)
            }
          }
      case None =>
        Future.failed(new Error(s"Could not test rule: ${rule.id}"))
    }
  }
}
