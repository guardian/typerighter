package controllers

import play.api.mvc._
import com.gu.pandomainauth.PublicSettings
import com.gu.typerighter.lib.PandaAuthentication
import scala.concurrent.ExecutionContext

/** The controller that handles the management of matcher rules.
  */
class AuditController(cc: ControllerComponents, val publicSettings: PublicSettings)(implicit
    ec: ExecutionContext
) extends AbstractController(cc)
    with PandaAuthentication {
  def index = ApiAuthAction { implicit request => Ok(views.html.audit()) }
}
