package controllers

import play.api.mvc._
import com.gu.typerighter.controllers.PandaAuthController
import com.gu.typerighter.lib.CommonConfig
import play.api.libs.json.Json
import services._
import typerighter.BuildInfo

/** The controller for the index pages.
  */
class HomeController(
    controllerComponents: ControllerComponents,
    matcherPool: MatcherPool,
    config: CommonConfig
) extends PandaAuthController(controllerComponents, config) {

  def index() = AuthAction {
    Ok(views.html.index())
  }

  def healthcheck() = Action {
    {
      val ruleCount = matcherPool.getCurrentRuleCount

      if (ruleCount == 0) {
        val errorMsg = "No rules found in S3"
        log.error(errorMsg)
        InternalServerError(
          Json.obj(
            "healthy" -> false,
            "error" -> errorMsg
          )
        )
      } else {
        Ok(Json.obj("healthy" -> true, "gitCommitId" -> BuildInfo.gitCommitId))
      }
    }
  }

  def oauthCallback = Action.async { implicit request =>
    processOAuthCallback()
  }
}
