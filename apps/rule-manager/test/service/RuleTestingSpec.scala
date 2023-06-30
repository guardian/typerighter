package service

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.gu.contentapi.client.model.v1.SearchResponse
import com.gu.typerighter.lib.{HMACClient, JsonHelpers}
import com.gu.typerighter.model.{CheckSingleRuleResult, Document, TextBlock}
import fixtures.RuleFixtures
import org.mockito.scalatest.IdiomaticMockito
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.BuiltInComponentsFromContext
import play.api.mvc._
import play.api.routing.Router
import play.api.routing.sird._
import play.core.server.Server
import play.api.test._
import play.filters.HttpFiltersComponents

import scala.concurrent.{ExecutionContext, Future}

class RuleTestingSpec extends AnyFlatSpec with Matchers with IdiomaticMockito {
  val as: ActorSystem = ActorSystem()
  implicit val materializer: Materializer = Materializer(as)
  private implicit val ec = ExecutionContext.global

  // Mock responses from our CAPI client and checker service when parsing responses
  def withRuleTestingClient[T](
      searchResponses: Seq[SearchResponse],
      matchResponses: Seq[Seq[CheckSingleRuleResult]]
  )(block: RuleTesting => T)(implicit ec: ExecutionContext): T = {
    val searchIterator = Iterator(searchResponses)
    val matchResponseIterator = Iterator(matchResponses)

    Server.withApplicationFromContext() { context =>
      new BuiltInComponentsFromContext(context) with HttpFiltersComponents {
        override def router: Router = Router.from { case GET(p"/checkSingle") =>
          Action { req =>
            Results.Ok
              .chunked(
                Source(matchResponseIterator.next()).map(result => JsonHelpers.toNDJson(result))
              )
              .as("application/json-seq")
          }
        }
      }.application
    } { implicit port =>
      WsTestClient.withClient { client =>
        val hmacClient = new HMACClient("TEST", secretKey = "🤫")
        val contentClient = mock[ContentClient]
        contentClient.searchContent(*, *, *, *) answers Future.successful(searchIterator.next())
        block(new RuleTesting(client, hmacClient, contentClient, ""))
      }
    }
  }

  behavior of "testRule"

  val exampleRule = RuleFixtures.createRandomRules(1).head
  val exampleDocuments = List(
    Document("test-document", List(TextBlock("id", "Example text", 0, 11)))
  )
  val exampleMatches = CheckSingleRuleResult(
    matches = List.empty,
    percentageRequestComplete = Some(100)
  )

  it should "handle an empty stream" in {
    val matchResponses = List(List.empty).iterator
    withRuleTestingClient(matchResponses = matchResponses) { client =>
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

    withRuleTestingClient(matches.iterator) { client =>
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
