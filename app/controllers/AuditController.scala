package controllers

import play.api.mvc._

import scala.concurrent.ExecutionContext

/**
 * The controller that handles the management of matcher rules.
 */
class AuditController(cc: ControllerComponents)(implicit ec: ExecutionContext)  extends AbstractController(cc) {
  def index = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.audit()).withHeaders((CONTENT_SECURITY_POLICY, "default-src 'self' 'unsafe-eval'"))
  }
}
