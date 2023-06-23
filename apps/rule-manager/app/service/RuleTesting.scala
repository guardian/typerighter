package service

import akka.stream.Materializer
import play.api.libs.ws.WSClient
import com.gu.typerighter.lib.{HMACClient, JsonHelpers, Loggable}
import com.gu.typerighter.model.{CheckSingleRule, CheckSingleRuleResult, Document}
import db.DbRuleDraft
import play.api.libs.json.{Format, JsError, JsSuccess, Json}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

case class TestRuleCapiQuery(
    queryStr: String,
    tags: List[String] = List.empty,
    sections: List[String] = List.empty,
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
)(implicit materializer: Materializer) extends Loggable {

  /** Test a rule against a given CAPI query. Return a list of matches from the checker
    * service endpoint as a stream of json-seq records.
    */
  def testRuleWithCapiQuery(
      ruleId: Int,
      query: TestRuleCapiQuery
  )(implicit ec: ExecutionContext) = {
    for {
      content <- contentClient.searchContent(query.queryStr, query.tags, query.sections)
      documents = content.results.map { result =>
        Document(
          result.id,
          result.blocks
            .flatMap(_.body)
            .getOrElse(Seq.empty)
            .flatMap(TextBlock.fromCAPIBlock)
            .toList
        )
      }
    } yield testRule(ruleId, documents.toList)
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
