package controllers

import play.api.mvc._
<<<<<<< HEAD
import com.gu.typerighter.controllers.PandaAuthController
=======
import com.gu.typerighter.controllers.AppAuthActions
>>>>>>> 7130b55a (Refactor configuration to pass pan-domain-config as a part of CommonConfig, and adjust controller configuration to suit HMACAuthActions)
import com.gu.typerighter.lib.CommonConfig
import play.api.libs.json.Json
import services._

/** The controller for the index pages.
  */
class HomeController(
<<<<<<< HEAD
    controllerComponents: ControllerComponents,
    matcherPool: MatcherPool,
    config: CommonConfig
) extends PandaAuthController(controllerComponents, config) {
=======
    val controllerComponents: ControllerComponents,
    matcherPool: MatcherPool,
    val config: CommonConfig
) extends BaseController
    with AppAuthActions {
>>>>>>> 7130b55a (Refactor configuration to pass pan-domain-config as a part of CommonConfig, and adjust controller configuration to suit HMACAuthActions)
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
