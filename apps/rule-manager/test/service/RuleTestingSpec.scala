package service

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.gu.typerighter.lib.{HMACClient, JsonHelpers}
import com.gu.typerighter.model.{CheckSingleRuleResult, Document, TextBlock}
import fixtures.RuleFixtures
import org.mockito.scalatest.IdiomaticMockito
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.mvc.Results.Ok
import play.api.routing.sird._
import play.core.server.Server
import play.api.test._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class RuleTestingSpec extends AnyFlatSpec with Matchers with IdiomaticMockito {

  val as: ActorSystem = ActorSystem()
  implicit val materializer: Materializer = Materializer(as)

  /** Mock responses from our checker service when parsing responses.
    *
    * The matches are passed as an iterator to allow us to assert their state when tests are
    * finished running – to check that they've been consumed.
    */
  def withRuleTestingClient[T](
      matchResponses: Iterator[Seq[CheckSingleRuleResult]]
  )(block: RuleTesting => T): T = {
    Server.withRouterFromComponents() { cs =>
      { case POST(p"/checkSingle") =>
        cs.defaultActionBuilder { _ =>
          Ok.chunked(
            Source(matchResponses.next()).map(result => JsonHelpers.toJsonSeq(result))
          ).as("application/json-seq")
        }
      }
    } { implicit port =>
      WsTestClient.withClient { client =>
        val hmacClient = new HMACClient("TEST", secretKey = "🤫")
        block(new RuleTesting(client, hmacClient, ""))
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
