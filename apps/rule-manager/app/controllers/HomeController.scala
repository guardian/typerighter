package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import com.gu.pandomainauth.PublicSettings
import com.gu.typerighter.lib.PandaAuthentication
import com.gu.typerighter.lib.Loggable

import _root_.db.RuleManagerDB
import com.gu.pandomainauth.PanDomainAuthSettingsRefresher

class HomeController(
  val controllerComponents: ControllerComponents,
  val db: RuleManagerDB,
  override val panDomainSettings: PanDomainAuthSettingsRefresher,
  override val wsClient: WSClient,
  override val authDomain: String
) extends BaseController with Loggable with AppAuthActions {

  def index() = AuthAction { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def healthcheck() = Action { implicit request: Request[AnyContent] =>
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

  def oauthCallback = Action.async { implicit request =>
    processOAuthCallback()
  }
}
