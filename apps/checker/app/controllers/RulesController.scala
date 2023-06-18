package controllers

<<<<<<< HEAD
import com.gu.typerighter.controllers.PandaAuthController
=======
import com.gu.typerighter.controllers.AppAuthActions
>>>>>>> 7130b55a (Refactor configuration to pass pan-domain-config as a part of CommonConfig, and adjust controller configuration to suit HMACAuthActions)
import com.gu.typerighter.lib.CommonConfig
import play.api.mvc._
import services._

/** The controller that handles the management of matcher rules.
  */
class RulesController(
<<<<<<< HEAD
    controllerComponents: ControllerComponents,
    matcherPool: MatcherPool,
    sheetId: String,
    ruleManagerUrl: String,
    config: CommonConfig
) extends PandaAuthController(controllerComponents, config) {
=======
    val controllerComponents: ControllerComponents,
    matcherPool: MatcherPool,
    sheetId: String,
    val ruleManagerUrl: String,
    val config: CommonConfig
) extends BaseController
    with AppAuthActions {
>>>>>>> 7130b55a (Refactor configuration to pass pan-domain-config as a part of CommonConfig, and adjust controller configuration to suit HMACAuthActions)

  def rules = APIAuthAction { implicit request: Request[AnyContent] =>
    Ok(
      views.html.rules(
        sheetId,
        matcherPool.getCurrentRules,
        matcherPool.getCurrentMatchers,
        ruleManagerUrl
      )
    )
  }
}
