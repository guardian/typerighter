package service

import akka.stream.Materializer
import play.api.libs.ws.WSClient
import com.gu.typerighter.lib.{HMACClient, JsonHelpers, Loggable}
import com.gu.typerighter.model.{CheckSingleRule, CheckSingleRuleResult, Document, RuleMatch}
import akka.NotUsed
import akka.stream.KillSwitches
import akka.stream.scaladsl.{Keep, MergeHub, Sink, Source}
import db.DbRuleDraft
import play.api.libs.json.{Format, JsError, JsSuccess, Json}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

case class TestRuleCapiQuery(
    queryStr: String,
    tags: List[String] = List.empty,
    sections: List[String] = List.empty
)

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
)(implicit materializer: Materializer)
    extends Loggable {

  /** Test a rule against a given CAPI query. Paginates through CAPI content to return matches
    * until:
    *   - `matchCount` is met.
    *   - `maxPageCount` is met.
    */
  def testRuleWithCapiQuery(
      ruleId: Int,
      query: TestRuleCapiQuery,
      matchCount: Int = 10,
      maxPageCount: Int = 20
  )(implicit ec: ExecutionContext): Source[RuleMatch, NotUsed] = {
    // The stream graph in this function looks like:
    // [Stream per document check][] ~> HubSink ~> ResultSink ~> ResultSource
    //
    // We must materialise a MergeHub to access its sink. We connect it to
    // this Sink/Source pair to provide a Source[RuleMatch, NotUsed] for the
    // function output.
    val (resultSink, resultSource) =
      Source
        .asSubscriber[CheckSingleRuleResult]
        .mapConcat { _.matches }
        .take(matchCount)
        // Connect the resultSink to the resultSource.
        // See https://discuss.lightbend.com/t/create-source-from-sink-and-vice-versa/605/6
        .toMat(Sink.asPublisher[RuleMatch](fanout = false))(Keep.both)
        .mapMaterializedValue { case (sub, pub) =>
          (Sink.fromSubscriber(sub), Source.fromPublisher(pub))
        }
        .run()

    // The MergeHub sink is responsible for receiving input from arbitrary numbers of
    // streams produced by `testRule`, and passing the output on to the `resultSink`.
    // `killSwitch` lets us manually terminate the stream.
    val (hubSink, killSwitch) = MergeHub
      .source[CheckSingleRuleResult](perProducerBufferSize = 16)
      .viaMat(KillSwitches.single)(Keep.both)
      .to(resultSink)
      .run()

    /** Check the next page of content, forwarding the results to the `hubSink` stream.
      */
    def checkNextPage: Int => Future[NotUsed] = (page: Int) => {
      log.info(s"Checking page $page/$maxPageCount")
      contentClient.searchContent(query.queryStr, query.tags, query.sections, page).flatMap {
        response =>
          val documents = response.results.map(Document.fromCapiContent).toList

          val streamCompleteCallbackSink = Sink.onComplete { _ =>
            log.info(s"Page $page complete")
            if (page == maxPageCount) {
              log.info(s"Checked $page pages, shutting down stream")
              killSwitch.shutdown()
            } else {
              checkNextPage(page + 1)
            }
          }

          // Stream the results into our hub sink.
          testRule(ruleId, documents).map { ruleSource =>
            ruleSource
              .alsoTo(streamCompleteCallbackSink)
              .runWith(hubSink)
          }
      }
    }

    checkNextPage(1)

    resultSource
  }

  /** Test a rule against the given list of documents. Return a list of matches from the checker
    * service endpoint as a stream of json-seq records.
    */
  def testRule(rule: DbRuleDraft, documents: List[Document])(implicit ec: ExecutionContext) = {
    val liveRule = rule.toLive("placeholder")

    RuleManager.liveDbRuleToCheckerRule(liveRule).toOption match {
      case Some(rule) =>
        val path = "/checkSingle"
        val headers = hmacClient.getHMACHeaders(path)
        val url = checkerUrl + path
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
                    case JsSuccess(value, _) => Some(value)
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
