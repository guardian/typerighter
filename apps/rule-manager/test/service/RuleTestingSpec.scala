package service

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.gu.contentapi.client.model.v1.SearchResponse
import com.gu.typerighter.lib.{HMACClient, JsonHelpers}
import com.gu.typerighter.model.{CheckSingleRuleResult, Document, TextBlock}
import fixtures.Rules
import org.mockito.scalatest.IdiomaticMockito
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.mvc.Results.Ok
import play.api.routing.sird._
import play.core.server.Server
import play.api.test._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class RuleTestingSpec extends AnyFlatSpec with Matchers with IdiomaticMockito {

  val as: ActorSystem = ActorSystem()
  implicit val materializer: Materializer = Materializer(as)

  // Mock responses from our CAPI client and checker service when parsing responses
  def withRuleTestingClient[T](
      searchResponses: Seq[SearchResponse],
      matchResponses: Seq[Seq[CheckSingleRuleResult]]
  )(block: RuleTesting => T): T = {
    val matchResponseIterator = matchResponses.iterator
    val searchResponseIterator = searchResponses.iterator

    Server.withRouterFromComponents() { cs =>
      {
        { case GET(p"/checkSingle") =>
          cs.defaultActionBuilder { _ =>
            Ok
              .chunked(
                Source(matchResponseIterator.nextOption().getOrElse(Seq.empty)).map(result =>
                  JsonHelpers.toNDJson(result)
                )
              )
              .as("application/json-seq")
          }
        }
      }
    } { implicit port =>
      WsTestClient.withClient { client =>
        val hmacClient = new HMACClient("TEST", secretKey = "🤫")
        val contentClient = mock[ContentClient]
        if (searchResponses.nonEmpty) {
          contentClient.searchContent(*, *, *, *) shouldReturn Future.successful(
            searchResponseIterator.next()
          )
        }

        block(new RuleTesting(client, hmacClient, contentClient, ""))
      }
    }
  }

  behavior of "testRule"

  val exampleRule = Rules.createRandomRules(1).head
  val exampleDocuments = List(
    Document("test-document", TextBlock.fromHtml("""<p>Example content</p>"""))
  )
  val exampleMatches = CheckSingleRuleResult(
    matches = List.empty,
    percentageRequestComplete = Some(100)
  )

  it should "handle an empty stream" in {
    withRuleTestingClient(List.empty, List.empty) { client =>
      val eventualResult =
        client.testRule(exampleRule, exampleDocuments).flatMap(_.runWith(Sink.seq))
      val result = Await.result(eventualResult, 10 seconds)

      result shouldBe List.empty
    }
  }

  it should "receive a stream with many results" in {
    val matches = List(
      List(
        exampleMatches.copy(percentageRequestComplete = Some(50)),
        exampleMatches
      )
    )

    withRuleTestingClient(List.empty, matches) { client =>
      val eventualResult =
        client.testRule(exampleRule, exampleDocuments).flatMap(_.runWith(Sink.seq))
      val result = Await.result(eventualResult, 60 seconds)

      result shouldBe Seq(
        CheckSingleRuleResult(List(), Some(50)),
        CheckSingleRuleResult(List(), Some(100))
      )
    }
  }
}
