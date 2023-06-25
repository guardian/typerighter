package service

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.gu.contentapi.client.model.v1.SearchResponse
import com.gu.typerighter.lib.{HMACClient, JsonHelpers}
import com.gu.typerighter.model.CheckSingleRuleResult
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

import scala.concurrent.ExecutionContext

class RuleManagerTest extends AnyFlatSpec with Matchers with IdiomaticMockito {
  val as: ActorSystem = ActorSystem()
  implicit val materializer: Materializer = Materializer(as)

  // Mock responses from our CAPI client and checker service when parsing responses
  def withRuleTestingClient[T](searchResponses: Seq[SearchResponse], matchResponses: Seq[Seq[CheckSingleRuleResult]])(block: RuleTesting => T)(implicit ec: ExecutionContext): T = {
    val searchIterator = Iterator(searchResponses)
    val matchResponseIterator = Iterator(matchResponses)

    Server.withApplicationFromContext() { context =>
      new BuiltInComponentsFromContext(context) with HttpFiltersComponents {
        override def router: Router = Router.from {
          case GET(p"/checkSingle") =>
            Action { req =>
              Results.Ok.chunked(
                Source(matchResponseIterator.next()).map(result => JsonHelpers.toNDJson(result))
              ).as("application/json-seq")
            }
        }
      }.application
    } { implicit port =>
      WsTestClient.withClient { client =>
        val hmacClient = new HMACClient("TEST", secretKey = "🤫")
        val contentClient = mock[ContentClient]
        contentClient.searchContent(*, *, *, *) answers searchIterator.next()
        block(new RuleTesting(client, hmacClient, contentClient,""))
      }
    }
  }
}
