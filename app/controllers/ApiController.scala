package controllers

import model.{ApiRequest, ApiResponse}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import services._

import scala.concurrent.{ExecutionContext, Future}

/**
  * The controller that handles API requests.
  */
class ApiController(
  cc: ControllerComponents,
  matcherPool: MatcherPool
)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  def check: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[ApiRequest].asEither match {
      case Right(check) =>
        matcherPool
          .check(check)
          .map { matches =>
            val response = ApiResponse(
              matches = matches,
              blocks = check.blocks,
              categoryIds = check.categoryIds.getOrElse(matcherPool.getCurrentCategories.map { _._1 })
            )
            Ok(Json.toJson(response))
          } recover {
          case e: Exception =>
            InternalServerError(Json.obj("error" -> e.getMessage))
        }
      case Left(error) =>
        Future.successful(BadRequest(s"Invalid request: $error"))
    }
  }

  def getCurrentCategories: Action[AnyContent] = Action {
      Ok(Json.toJson(matcherPool.getCurrentCategories.map(_._2)))
  }
}
