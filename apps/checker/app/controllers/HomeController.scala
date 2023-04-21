package controllers

import play.api.mvc._
import com.gu.pandomainauth.PublicSettings
import com.gu.typerighter.lib.PandaAuthentication
import play.api.libs.json.Json
import services._

/** The controller for the index pages.
  */
class HomeController(
    cc: ControllerComponents,
    matcherPool: MatcherPool,
    val publicSettings: PublicSettings
) extends AbstractController(cc)
    with PandaAuthentication {
  def index() = ApiAuthAction {
    Ok(views.html.index())
  }

  def healthcheck() = Action {
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
