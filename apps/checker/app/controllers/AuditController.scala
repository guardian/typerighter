package controllers

import play.api.mvc._
<<<<<<< HEAD
import com.gu.typerighter.controllers.PandaAuthController
=======
import com.gu.typerighter.controllers.AppAuthActions
>>>>>>> 7130b55a (Refactor configuration to pass pan-domain-config as a part of CommonConfig, and adjust controller configuration to suit HMACAuthActions)
import com.gu.typerighter.lib.CommonConfig

/** The controller that handles the management of matcher rules.
  */
<<<<<<< HEAD
class AuditController(controllerComponents: ControllerComponents, config: CommonConfig)
    extends PandaAuthController(controllerComponents, config) {
=======
class AuditController(
    val controllerComponents: ControllerComponents,
    val config: CommonConfig
) extends BaseController
    with AppAuthActions {
>>>>>>> 7130b55a (Refactor configuration to pass pan-domain-config as a part of CommonConfig, and adjust controller configuration to suit HMACAuthActions)
  def index = APIAuthAction { Ok(views.html.audit()) }
}
