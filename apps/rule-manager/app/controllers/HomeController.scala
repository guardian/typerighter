package controllers

import play.api.mvc._
import play.api.libs.json.Json
import com.gu.typerighter.lib.Loggable
import db.DB
import utils.PermissionsHandler
import com.gu.permissions.PermissionDefinition
import com.gu.typerighter.controllers.PandaAuthController
import typerighter.BuildInfo
import utils.RuleManagerConfig

class HomeController(
    controllerComponents: ControllerComponents,
    db: DB,
    val config: RuleManagerConfig
) extends PandaAuthController(controllerComponents, config)
    with Loggable
    with PermissionsHandler {

  def telemetryUrl: String =
    s"https://user-telemetry.${config.stageDomain}/guardian-tool-accessed?app=typerighter-manager"

  def index(path: String) = AuthAction { request =>
    Ok(
      views.html.index(
        config.stage,
        request.user,
        userAndPermissionsToJson(
          request.user,
          List(PermissionDefinition("manage_rules", "typerighter"))
        ),
        telemetryUrl
      )
    )
  }

  def healthcheck() = Action {
    try {
      db.connectionHealthy()
      Ok(Json.obj("healthy" -> true, "gitCommitId" -> BuildInfo.gitCommitId))
    } catch {
      case e: Throwable =>
        log.error("Healthcheck failed", e)
        InternalServerError(
          Json.obj(
            "healthy" -> false,
            "error" -> e.getMessage(),
            "gitCommitId" -> BuildInfo.gitCommitId
          )
        )
    }
  }

  def editPermissionCheck() = AuthAction { implicit request =>
    hasPermission(request.user, PermissionDefinition("manage_rules", "typerighter")) match {
      case true  => Ok("Permission granted")
      case false => Unauthorized("You don't have permission to edit rules")
    }
  }

  def oauthCallback = Action.async { implicit request =>
    processOAuthCallback()
  }
}
