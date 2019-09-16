package controllers

import model.Check
import actors.WsCheckQueryActor
import akka.actor.ActorSystem
import play.api.libs.json.{JsValue, Json}
import play.api.libs.streams.ActorFlow
import play.api.mvc._
import services._
import rules.RuleResource

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.streams.ActorFlow
import akka.stream.Materializer

/**
  * The controller that handles API requests.
  */
class ApiController(
    cc: ControllerComponents, 
    validatorPool: ValidatorPool,
    ruleResource: RuleResource
)(implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer)
    extends AbstractController(cc) {
  def check: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[Check].asEither match {
      case Right(check) =>
        validatorPool
          .check(check)
          .map { results =>
            val json = Json.obj(
              "results" -> Json.toJson(results)
            )
            Ok(json)
          } recover {
          case e: Exception =>
            InternalServerError(Json.obj("error" -> e.getMessage))
        }
      case Left(error) =>
        Future.successful(BadRequest(s"Invalid request: $error"))
    }
  }

  def checkWs = WebSocket.accept[JsValue, JsValue] { request =>
    ActorFlow.actorRef { out =>
      WsCheckQueryActor.props(out, validatorPool)
    }
  }

  def getCurrentCategories: Action[AnyContent] = Action {
      Ok(Json.toJson(validatorPool.getCurrentCategories.map(_._2)))
  }
}
