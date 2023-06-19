package service

import akka.stream.Materializer
import akka.stream.scaladsl.JsonFraming
import play.api.libs.ws.WSClient
import com.gu.typerighter.lib.{HMACClient, Loggable}
import com.gu.typerighter.model.{CheckSingleRule, CheckSingleRuleResult, CheckerRule, Document}
import play.api.libs.json.Json

import java.util.UUID
import scala.concurrent.ExecutionContext

class RuleTesting(
    ws: WSClient,
    hmacClient: HMACClient,
    materializer: Materializer,
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
          .map(lines =>
            for {
              line <- lines
            } yield {
              Json.parse(line).validateOpt[CheckSingleRuleResult]
            }
          )
      }
  }
}
