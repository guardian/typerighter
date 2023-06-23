package service

import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, MergeHub, Sink, Source}
import akka.util.ByteString
import play.api.libs.ws.WSClient
import com.gu.typerighter.lib.{HMACClient, Loggable}
import com.gu.typerighter.model.{CheckSingleRule, Document, TextBlock}
import db.DbRuleDraft
import play.api.libs.json.{Format, Json}

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

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
    * service endpoint as streamed NDJSON.
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

  /** Test a rule against the given list of documents. Return a list of matches from the
    * checker service endpoint as streamed NDJSON.
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

        val publisher = Sink.asPublisher[ByteString](false)

        val (mergedSink, mergedSource) =
          Source
            .asSubscriber[ByteString]
            .take(1)
            .toMat(publisher)(Keep.both)
            .mapMaterializedValue {
              case (sub, pub) => (Sink.fromSubscriber(sub), Source.fromPublisher(pub))
            }.run()

        val hubSource = MergeHub
          .source[ByteString](perProducerBufferSize = 16)
          .to(mergedSink)
          .run()

        ws.url(s"https://$checkerUrl$path")
          .withHttpHeaders(headers: _*)
          .withBody(Json.toJson(checkSingleRule))
          .stream()
          .map { response =>
            response.bodyAsSource.
              runWith(hubSource)
          }

        Success(mergedSource)
      case None =>
        Failure(new Error(s"Could not test rule: $ruleId"))
    }
  }
}
