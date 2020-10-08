package controllers

import com.gu.pandomainauth.PublicSettings
import model.{Check, MatcherResponse}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import services.{PandaAuthentication, MatcherPool}

import scala.concurrent.{ExecutionContext, Future}

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
        matcherPool
          .check(check)
          .map { matches =>
            val response = MatcherResponse(
              matches = matches,
              blocks = check.blocks,
              categoryIds = matches.map(_.rule.category.id).distinct
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

  def getCurrentCategories: Action[AnyContent] = ApiAuthAction {
      Ok(Json.toJson(matcherPool.getCurrentCategories.map(_._2)))
  }
}
