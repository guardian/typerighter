package controllers

import play.api.mvc._

import scala.concurrent.ExecutionContext

/**
 * The controller that handles the management of matcher rules.
 */
class AuditController(cc: ControllerComponents)(implicit ec: ExecutionContext)  extends AbstractController(cc) {
  def index = Action { implicit request => Ok(views.html.audit()) }
}
