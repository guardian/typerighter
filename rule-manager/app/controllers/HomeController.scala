package controllers

import play.api._
import play.api.mvc._
import _root_.db.RuleManagerDB
import play.api.libs.json.Json

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */

class HomeController(val controllerComponents: ControllerComponents, db: RuleManagerDB) extends BaseController with Logging {

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def healthcheck() = Action { implicit request: Request[AnyContent] =>
    try {
      db.connectionHealthy()
      Ok(Json.obj("healthy" -> true))
    } catch {
      case e: Throwable =>
        logger.error("Healthcheck failed", e)
        InternalServerError(
          Json.obj(
            "healthy" -> false,
            "error" -> e.getMessage()
          )
        )
    }
  }
}
