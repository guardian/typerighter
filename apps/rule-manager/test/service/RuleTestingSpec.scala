package service

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.gu.contentapi.client.model.v1.SearchResponse
import com.gu.typerighter.lib.{ContentClient, HMACClient, JsonHelpers}
import com.gu.typerighter.model.{CheckSingleRuleResult, Document, TextBlock}
import com.gu.typerighter.fixtures.RuleMatchFixtures
import fixtures.{CAPIFixtures, RuleFixtures}
import org.mockito.Mockito.when
import org.mockito.scalatest.IdiomaticMockito
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.mvc.Results.Ok
import play.api.routing.sird._
import play.core.server.Server
import play.api.test._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

class RuleTestingSpec extends AnyFlatSpec with Matchers with IdiomaticMockito {
  val as: ActorSystem = ActorSystem()
  implicit val materializer: Materializer = Materializer(as)
  private implicit val ec = ExecutionContext.global

  /** Mock responses from our CAPI client and checker service when parsing responses.
    *
    * The responses and matches are passed as iterators to allow us to assert their state when tests
    * are finished running – to check that they've been consumed.
    */
  def withRuleTestingClient[T](
      searchResponses: Iterator[SearchResponse] = List.empty.iterator,
      matchResponses: Iterator[Seq[CheckSingleRuleResult]]
  )(block: RuleTesting => T): T = {
    Server.withRouterFromComponents() { cs =>
      { case GET(p"/checkSingle") =>
        cs.defaultActionBuilder { _ =>
          Ok.chunked(
            Source(matchResponses.next()).map(result => JsonHelpers.toJsonSeq(result))
          ).as("application/json-seq")
        }
      }
    } { implicit port =>
      WsTestClient.withClient { client =>
        val hmacClient = new HMACClient("TEST", secretKey = "🤫")
        val contentClient = mock[ContentClient]
        if (searchResponses.nonEmpty) {
          when(contentClient.searchContent(any, any, any, any)(any)) thenAnswer (_ =>
            Future.successful(searchResponses.next())
          )
        }

        block(new RuleTesting(client, hmacClient, contentClient, ""))
      }
    }
  }

  behavior of "testRule"

  val exampleRule = RuleFixtures.createRandomRules(1).head
  val exampleDocuments = List(
    Document("test-document", List(TextBlock("id", "Example text", 0, 11)))
  )
  val emptyCheckResult = CheckSingleRuleResult(
    matches = List.empty,
    percentageRequestComplete = Some(100)
  )
  val checkResultWithSingleMatch =
    CheckSingleRuleResult(List(RuleMatchFixtures.getRuleMatch(0, 10)), Some(100))
  val exampleQuery = TestRuleCapiQuery("An example query")

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
        emptyCheckResult.copy(percentageRequestComplete = Some(50)),
        emptyCheckResult
      )
    ).iterator

    withRuleTestingClient(List.empty.iterator, matches) { client =>
      val eventualResult =
        client.testRule(exampleRule, exampleDocuments).flatMap(_.runWith(Sink.seq))
      val result = Await.result(eventualResult, 60 seconds)

      result shouldBe Seq(
        CheckSingleRuleResult(List(), Some(50)),
        CheckSingleRuleResult(List(), Some(100))
      )
    }
  }

  behavior of "testRuleWithCapiQuery"

  it should "poll CAPI up to the page count in the absence of matches" in {
    val desiredPageCount = 5
    val responses = List.fill(desiredPageCount)(CAPIFixtures.searchResponseWithBodyField).iterator
    val matches = List.fill(desiredPageCount)(List(emptyCheckResult)).iterator

    withRuleTestingClient(searchResponses = responses, matchResponses = matches) { client =>
      val eventualResult =
        client
          .testRuleWithCapiQuery(
            rule = exampleRule,
            query = exampleQuery,
            matchCount = 5,
            maxPageCount = desiredPageCount
          )
          .runWith(Sink.seq)
      val result = Await.result(eventualResult, 60 seconds)

      // We should have exhausted both iterators, to show we
      // haven't asked either CAPI or the matcher service for
      // more than we need
      matches.hasNext shouldBe false
      responses.hasNext shouldBe false

      result shouldBe Seq(
        PaginatedCheckRuleResult(1, 5, CheckSingleRuleResult(List.empty, Some(100))),
        PaginatedCheckRuleResult(2, 5, CheckSingleRuleResult(List.empty, Some(100))),
        PaginatedCheckRuleResult(3, 5, CheckSingleRuleResult(List.empty, Some(100))),
        PaginatedCheckRuleResult(4, 5, CheckSingleRuleResult(List.empty, Some(100))),
        PaginatedCheckRuleResult(5, 5, CheckSingleRuleResult(List.empty, Some(100)))
      )
    }
  }

  it should "stop polling CAPI once the match count is reached" in {
    val desiredMatchCount = 6
    val responses = List.fill(4)(CAPIFixtures.searchResponseWithBodyField).iterator
    // Respond with seven matches over three documents, hitting our match limit
    val matches = List(
      List.fill(desiredMatchCount / 2)(checkResultWithSingleMatch),
      List.fill(desiredMatchCount / 2)(checkResultWithSingleMatch),
      List.fill(1)(checkResultWithSingleMatch)
    )
    val matchesIterator = matches.iterator

    withRuleTestingClient(searchResponses = responses, matchResponses = matchesIterator) { client =>
      val eventualResult =
        client
          .testRuleWithCapiQuery(
            rule = exampleRule,
            query = exampleQuery,
            matchCount = desiredMatchCount,
            maxPageCount = 6
          )
          .runWith(Sink.seq)
      val result = Await.result(eventualResult, 60 seconds)

      // We should have exhausted both iterators, to show we
      // haven't asked either CAPI or the matcher service for
      // more than we need.
      matchesIterator.hasNext shouldBe false
      responses.hasNext shouldBe false

      val expectedResult = matches.take(2).flatten.zipWithIndex.map { case (result, index) =>
        PaginatedCheckRuleResult(if (index < 3) 1 else 2, 6, result)
      }

      result shouldBe expectedResult
    }
  }
}
