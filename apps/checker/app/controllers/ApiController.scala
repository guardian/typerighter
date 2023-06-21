package controllers

import akka.stream.scaladsl.Sink
import com.gu.typerighter.controllers.PandaAuthController
import model.Check
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import services.{MatcherPool, MatcherProvisionerService}
import com.gu.typerighter.lib.CommonConfig
import com.gu.typerighter.model.CheckSingleRule

import scala.concurrent.{ExecutionContext, Future, Promise}
import utils.{JsonHelpers, Timer}

import scala.util.{Failure, Success}

/** The controller that handles API requests.
  */
class ApiController(
    controllerComponents: ControllerComponents,
    matcherPool: MatcherPool,
    matcherProvisionerService: MatcherProvisionerService,
    config: CommonConfig
)(implicit ec: ExecutionContext)
    extends PandaAuthController(controllerComponents, config) {
  def check: Action[JsValue] = APIAuthAction.async(parse.json) { request =>
    request.body.validate[Check].asEither match {
      case Right(check) =>
        val eventuallyResult =
          Timer.timeAsync("ApiController.check", check.toMarker(request.user)) {
            matcherPool.check(check)
          }

        eventuallyResult.map { result =>
          Ok(Json.toJson(result))
        } recover { case e: Exception =>
          InternalServerError(Json.obj("error" -> e.getMessage))
        }
      case Left(error) => Future.successful(BadRequest(s"Invalid request: $error"))
    }
  }

  def checkStream = APIAuthAction[JsValue](parse.json) { request =>
    request.body.validate[Check].asEither match {
      case Right(check) =>
        // A promise to let us listen for the end of the stream, and log when the request is complete.
        val timerPromise = Promise[Unit]()
        Timer.timeAsync("ApiController.checkStream", check.toMarker(request.user))(
          timerPromise.future
        )

        val resultStream = matcherPool
          .checkStream(check)
          .map(result => JsonHelpers.toNDJson(result))
          .alsoTo(completeStreamWithPromise(timerPromise))

        Ok.chunked(resultStream).as("application/json-seq")
      case Left(error) => BadRequest(s"Invalid request: $error")
    }
  }

  def checkSingleRule = APIHMACAuthAction[JsValue](parse.json) { request =>
    request.body.validate[CheckSingleRule].asEither match {
      case Right(check) =>
        matcherProvisionerService.getMatcherForRule(check.rule) match {
          case Right(matcher) =>
            val timerPromise = Promise[Unit]()
            Timer.timeAsync("ApiController.checkSingle", check.toMarker(request.user.email))(
              timerPromise.future
            )

            val resultStream = matcherPool
              .checkSingle(check, matcher)
              .map(result => JsonHelpers.toNDJson(result))
              .alsoTo(completeStreamWithPromise(timerPromise))

            Ok.chunked(resultStream).as("application/json-seq")
          case Left(errors) =>
            BadRequest(s"Could not create rule: ${errors.map(_.getMessage()).mkString(", ")}")
        }
      case Left(error) => BadRequest(s"Invalid request: $error")
    }
  }

  def getCurrentCategories: Action[AnyContent] = APIAuthAction {
    Ok(Json.toJson(matcherPool.getCurrentCategories))
  }

  private def completeStreamWithPromise(promise: Promise[Unit]) =
    Sink.onComplete {
      case Success(_)  => promise.complete(Success(()))
      case Failure(ex) => promise.failure(ex)
    }
}
