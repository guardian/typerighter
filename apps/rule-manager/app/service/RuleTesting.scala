package service

import play.api.libs.ws.WSClient
import com.gu.typerighter.lib.{HMACClient, Loggable}
import com.gu.typerighter.model.{CheckSingleRule, CheckerRule, Document}
import play.api.libs.json.Json

import java.util.UUID

class RuleTesting(
    ws: WSClient,
    hmacClient: HMACClient,
    checkerUrl: String
) extends Loggable {
  /** Test a rule against the given list of documents. Return a list of matches from the checker
    * service endpoint as streamed NDJSON.
    */
  def testRule(ruleId: Int, documents: List[Document])(implicit ec: ExecutionContext) = {
    DbRuleDraft.find(ruleId).flatMap { draftRule =>
      val liveRule = draftRule.toLive("placeholder")
      RuleManager.liveDbRuleToCheckerRule(liveRule).toOption
    } match {
      case Some(rule) =>
        val path = "/checkSingle"
        val headers = hmacClient.getHMACHeaders(path)
        val requestId = UUID.randomUUID().toString()
        val checkSingleRule = CheckSingleRule(
          requestId = requestId,
          documents = documents,
          rule = rule
        )

        ws.url(s"https://$checkerUrl$path")
          .withHttpHeaders(headers: _*)
          .withBody(Json.toJson(checkSingleRule))
          .stream()
          .map {
            _.bodyAsSource
              .via(JsonHelpers.NDJsonDelimiter)
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
        Future.failed(new Error(s"Could not test rule: $ruleId"))
    }
>>>>>>> 2eb177c4 (Add mock for test)
  }
}
