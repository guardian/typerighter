package controllers

import play.api.mvc._
import com.gu.pandomainauth.PublicSettings
import com.gu.typerighter.lib.PandaAuthentication

/**
 * The controller that handles the management of matcher rules.
 */
class AuditController(cc: ControllerComponents, val publicSettings: PublicSettings) extends AbstractController(cc) with PandaAuthentication {
  def index = ApiAuthAction {  Ok(views.html.audit()) }
}
