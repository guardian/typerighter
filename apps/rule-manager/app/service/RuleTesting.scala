package service

import play.api.libs.ws.WSClient
import com.gu.typerighter.lib.{HMACClient, Loggable}
import com.gu.typerighter.model.{CheckSingleRule, CheckSingleRuleResult, CheckerRule, Document}
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
    * service endpoint as streamed NDJSON.
    */
  def testRule(rule: DbRuleDraft, documents: List[Document])(implicit ec: ExecutionContext) = {
    val liveRule = rule.toLive("placeholder")

    RuleManager.liveDbRuleToCheckerRule(liveRule).toOption match {
      case Some(rule) =>
        val path = "/checkSingle"
        val headers = hmacClient.getHMACHeaders(path)
        val requestId = UUID.randomUUID().toString()
        val checkSingleRule = CheckSingleRule(
          requestId = requestId,
          documents = documents,
          rule = rule
        )

        ws.url(s"$checkerUrl$path")
          .withHttpHeaders(headers: _*)
          .withBody(Json.toJson(checkSingleRule))
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
