package controllers

import scala.collection.JavaConverters._
import com.gu.pandomainauth.PublicSettings
import model.{Check, MatcherResponse}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import services.{PandaAuthentication, MatcherPool}

import scala.concurrent.{ExecutionContext, Future}
import utils.Timer
import net.logstash.logback.marker.Markers

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
        val markers = check.toMarker
        markers.add(Markers.appendEntries(Map("userEmail" -> request.user.email).asJava))
        val eventuallyMatches = Timer.timeAsync("ApiController.check", markers) {
          matcherPool.check(check)
        }

        eventuallyMatches.map { matches =>
          val response = MatcherResponse(
            matches = matches,
            blocks = check.blocks,
            categoryIds = matches.map(_.rule.category.id).toSet
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
      Ok(Json.toJson(matcherPool.getCurrentCategories))
  }
}
