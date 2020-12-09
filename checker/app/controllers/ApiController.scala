package controllers

import akka.stream.scaladsl.{Flow, Sink, Source}
import scala.collection.JavaConverters._
import com.gu.pandomainauth.PublicSettings
import model.{Check, CheckResponse}
import actor.{WsCheckActor}
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import services.{PandaAuthentication, MatcherPool}

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.streams.ActorFlow
import utils.Timer
import net.logstash.logback.marker.Markers
import akka.stream.Materializer
import akka.actor.ActorSystem

/**
  * The controller that handles API requests.
  */
class ApiController(
  cc: ControllerComponents,
  matcherPool: MatcherPool,
  val publicSettings: PublicSettings
)(implicit ec: ExecutionContext, mat: Materializer, system: ActorSystem) extends AbstractController(cc) with PandaAuthentication {

  def check: Action[JsValue] = ApiAuthAction.async(parse.json) { request =>
    request.body.validate[Check].asEither match {
      case Right(check) =>
        val eventuallyResult = Timer.timeAsync("ApiController.check", check.toMarker(request.user)) {
          matcherPool.check(check)
        }

        eventuallyResult.map { result =>
          Ok(Json.toJson(CheckResponse.fromCheckResult(result)))
        } recover {
          case e: Exception => InternalServerError(Json.obj("error" -> e.getMessage))
        }
      case Left(error) => Future.successful(BadRequest(s"Invalid request: $error"))
    }
  }

  def checkStream = WebSocket.acceptOrResult[JsValue, JsValue] { request =>
    ApiAuthAction.toSocket(request) { (user, _) =>
      ActorFlow.actorRef { out =>
        WsCheckActor.props(out, matcherPool, user)
      }
    }
  }

  def getCurrentCategories: Action[AnyContent] = ApiAuthAction { request =>
    Ok(Json.toJson(matcherPool.getCurrentCategories))
  }
}
