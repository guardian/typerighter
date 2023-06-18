package controllers

import play.api.mvc._
import com.gu.typerighter.controllers.AppAuthActions
import com.gu.typerighter.lib.CommonConfig

/** The controller that handles the management of matcher rules.
  */
class AuditController(
    val controllerComponents: ControllerComponents,
    val config: CommonConfig
) extends BaseController
    with AppAuthActions {
  def index = APIAuthAction { Ok(views.html.audit()) }
}
