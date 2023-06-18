package controllers

import com.gu.typerighter.controllers.PandaAuthController
import com.gu.typerighter.lib.CommonConfig
import play.api.mvc._
import services._

/** The controller that handles the management of matcher rules.
  */
class RulesController(
    controllerComponents: ControllerComponents,
    matcherPool: MatcherPool,
    sheetId: String,
    ruleManagerUrl: String,
    config: CommonConfig
) extends PandaAuthController(controllerComponents, config) {

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
