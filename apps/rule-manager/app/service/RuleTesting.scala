package service

import play.api.libs.ws.WSClient
import com.gu.typerighter.lib.{HMACClient, JsonHelpers, Loggable}
import com.gu.typerighter.model.{CheckSingleRule, CheckSingleRuleResult, CheckerRule, Document}
import db.DbRuleDraft
import play.api.libs.json.{JsError, JsSuccess, Json}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class RuleTesting(
    ws: WSClient,
    hmacClient: HMACClient,
    contentClient: ContentClient,
    checkerUrl: String
) extends Loggable {
  def testRule(
      rule: CheckerRule,
      queryStr: String,
      tags: List[String] = List.empty,
      sections: List[String] = List.empty
  )(implicit ec: ExecutionContext) = {
    for {
      content <- contentClient.searchContent(queryStr, tags, sections)
    } yield {
      content.results.map { result =>
        Document(
          result.id,
          result.blocks
            .flatMap(_.body)
            .getOrElse(Seq.empty)
            .flatMap(TextBlock.fromCAPIBlock)
            .toList
        )
      }
    }
  }

  def testRule(rule: CheckerRule, documents: List[Document]) = {
    val path = "/checkSingle"
    val headers = hmacClient.getHMACHeaders(path)
    val requestId = UUID.randomUUID().toString()
    val checkSingleRule = CheckSingleRule(
      requestId = requestId,
      documents = documents,
      rule = rule
    )

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
