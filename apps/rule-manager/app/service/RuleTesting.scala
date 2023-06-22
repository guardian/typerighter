package service

import play.api.libs.ws.WSClient
import com.gu.typerighter.lib.{HMACClient, Loggable}
import com.gu.typerighter.model.{CheckSingleRule, CheckerRule, Document, TextBlock}
import play.api.libs.json.Json

import java.util.UUID
import scala.concurrent.ExecutionContext

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

    ws.url(s"https://$checkerUrl$path")
      .withHttpHeaders(headers: _*)
      .withBody(Json.toJson(checkSingleRule))
      .stream()
  }
}
