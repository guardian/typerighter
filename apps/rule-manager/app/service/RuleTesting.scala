package service

import akka.stream.scaladsl.JsonFraming
import play.api.libs.ws.WSClient
import com.gu.typerighter.lib.{HMACClient, Loggable}
import com.gu.typerighter.model.{CheckSingleRule, CheckSingleRuleResult, CheckerRule, Document}
import play.api.libs.json.{JsError, JsSuccess, Json}

import java.util.UUID
import scala.concurrent.ExecutionContext

class RuleTesting(
    ws: WSClient,
    hmacClient: HMACClient,
    checkerUrl: String
) extends Loggable {
  def testRule(rule: CheckerRule, documents: List[Document])(implicit ec: ExecutionContext) = {
    val headers = hmacClient.getHMACHeaders(checkerUrl)
    val requestId = UUID.randomUUID().toString()
    val checkSingleRule = CheckSingleRule(
      requestId = requestId,
      documents = documents,
      rule = rule
    )

    ws.url(checkerUrl)
      .withHttpHeaders(headers: _*)
      .withBody(Json.toJson(checkSingleRule))
      .stream()
      .map { response =>
        response.bodyAsSource
          .via(JsonFraming.objectScanner(Int.MaxValue))
          .fold(Seq.empty[String]) { case (acc, entry) =>
            acc ++ Seq(entry.utf8String)
          }
          .map { lines =>
            lines.flatMap { line =>
              Json.parse(line).validate[CheckSingleRuleResult] match {
                case JsSuccess(result, _) => Some(result)
                case JsError(errors) =>
                  log.error(s"Could not parse response line from checker: ${errors
                      .map(error => s"${error._1.toJsonString}: ${error._2.map(_.toString())}")}")
                  None
              }
            }
          }
      }
  }
}
