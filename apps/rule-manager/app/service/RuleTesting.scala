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
