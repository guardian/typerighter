package controllers

import com.gu.pandomainauth.PublicSettings
import model.Check
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import services.MatcherPool
import com.gu.typerighter.lib.PandaAuthentication

import scala.concurrent.{ExecutionContext, Future}
import utils.Timer

/**
  * The controller that handles API requests.
  */
class ApiController(
  cc: ControllerComponents,
  matcherPool: MatcherPool,
  val publicSettings: PublicSettings
)(implicit ec: ExecutionContext) extends AbstractController(cc) with PandaAuthentication {
  def check: Action[JsValue] = ApiAuthAction.async(parse.json) { request =>
    request.body.validate[Check].asEither match {
      case Right(check) =>
        val eventuallyResult = Timer.timeAsync("ApiController.check", check.toMarker(request.user)) {
          matcherPool.check(check)
        }

        eventuallyResult.map { result =>
          Ok(Json.toJson(result))
        } recover {
          case e: Exception => InternalServerError(Json.obj("error" -> e.getMessage))
        }
      case Left(error) => Future.successful(BadRequest(s"Invalid request: $error"))
    }
  }

  def checkStream = ApiAuthAction[JsValue](parse.json) { request =>
    request.body.validate[Check].asEither match {
      case Right(check) =>
        val resultStream = matcherPool.checkStream(check).map(result => {
          Json.toJson(result).toString() + 31.toChar
        })
        Ok.chunked(resultStream).as("application/json-seq")
      case Left(error) => BadRequest(s"Invalid request: $error")
    }
  }

  def getCurrentCategories: Action[AnyContent] = ApiAuthAction {
    Ok(Json.toJson(matcherPool.getCurrentCategories))
  }
}
