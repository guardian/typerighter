package controllers

import play.api.mvc._
import com.gu.typerighter.controllers.AppAuthActions
import com.gu.typerighter.lib.CommonConfig
import play.api.libs.json.Json
import services._

/** The controller for the index pages.
  */
class HomeController(
    val controllerComponents: ControllerComponents,
    matcherPool: MatcherPool,
    val config: CommonConfig
) extends BaseController
    with AppAuthActions {
  def index() = APIAuthAction {
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
