package controllers

import play.api.mvc._
import com.gu.typerighter.controllers.PandaAuthController
import com.gu.typerighter.lib.CommonConfig

/** The controller that handles the management of matcher rules.
  */
class AuditController(controllerComponents: ControllerComponents, config: CommonConfig)
    extends PandaAuthController(controllerComponents, config) {
  def index = APIAuthAction { Ok(views.html.audit()) }
}
