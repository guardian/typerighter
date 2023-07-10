package controllers

import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import com.gu.typerighter.lib.Loggable

import _root_.db.DB
import com.gu.pandomainauth.PanDomainAuthSettingsRefresher
import utils.PermissionsHandler
import com.gu.permissions.PermissionDefinition
import utils.RuleManagerConfig

class HomeController(
    val controllerComponents: ControllerComponents,
    val db: DB,
    override val panDomainSettings: PanDomainAuthSettingsRefresher,
    override val wsClient: WSClient,
    override val config: RuleManagerConfig
) extends BaseController
    with Loggable
    with AppAuthActions
    with PermissionsHandler {

  def index(path: String) = AuthAction { request =>
    Ok(
      views.html.index(
        config.stage,
        request.user,
        userAndPermissionsToJson(
          request.user,
          List(PermissionDefinition("manage_rules", "typerighter"))
        )
      )
    )
  }

  def healthcheck() = Action {
    try {
      db.connectionHealthy()
      Ok(Json.obj("healthy" -> true))
    } catch {
      case e: Throwable =>
        log.error("Healthcheck failed", e)
        InternalServerError(
          Json.obj(
            "healthy" -> false,
            "error" -> e.getMessage()
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
