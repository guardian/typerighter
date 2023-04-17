package controllers

import play.api.mvc._
import com.gu.pandomainauth.PublicSettings
import com.gu.typerighter.lib.PandaAuthentication
import play.api.libs.json.Json
import services._

import scala.concurrent.ExecutionContext

/** The controller for the index pages.
  */
class HomeController(
    cc: ControllerComponents,
    matcherPool: MatcherPool,
    val publicSettings: PublicSettings
)(implicit
    ec: ExecutionContext
) extends AbstractController(cc)
    with PandaAuthentication {
  def index() = ApiAuthAction { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def healthcheck() = Action { implicit request: Request[AnyContent] =>
    {
      val rules = matcherPool.getCurrentRules

      if (rules.isEmpty) {
        val errorMsg = "No rules found in S3"
        log.error(errorMsg)
        InternalServerError(
          Json.obj(
            "healthy" -> false,
            "error" -> errorMsg
          )
        )
      } else {
        Ok(Json.obj("healthy" -> true))
      }
    }
  }
}
