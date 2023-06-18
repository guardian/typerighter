package controllers

import com.gu.typerighter.controllers.AppAuthActions
import com.gu.typerighter.lib.CommonConfig
import play.api.mvc._
import services._

/** The controller that handles the management of matcher rules.
  */
class RulesController(
    val controllerComponents: ControllerComponents,
    matcherPool: MatcherPool,
    sheetId: String,
    val ruleManagerUrl: String,
    val config: CommonConfig
) extends BaseController
    with AppAuthActions {

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
