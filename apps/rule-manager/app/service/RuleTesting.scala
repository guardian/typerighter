package service

import play.api.libs.ws.WSClient
import com.gu.typerighter.lib.{HMACClient, JsonHelpers, Loggable}
import com.gu.typerighter.model.{CheckSingleRule, CheckSingleRuleResult, Document}
import db.DbRuleDraft
import play.api.libs.json.{JsError, JsSuccess, Json}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class RuleTesting(
    ws: WSClient,
    hmacClient: HMACClient,
    checkerUrl: String
) extends Loggable {

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

        log.info(s"Fetching results for ${documents.size} document(s) from checker service $url with body $body")

        ws.url(url)
          .withHttpHeaders(headers: _*)
          .withMethod("POST")
          .withBody(body)
          .stream()
          .map {
            _.bodyAsSource
              .via(JsonHelpers.JsonSeqFraming)
              .mapConcat { str =>
                Json.parse(str.utf8String).validate[CheckSingleRuleResult] match {
                  case JsSuccess(value, _) => Some(value)
                  case JsError(error) =>
                    log.error(s"Error parsing checker result: ${error.toString}")
                    None
                }
              }
          }
      case None =>
        Future.failed(new Error(s"Could not test rule: ${rule.id}"))
    }
  }
}
