package controllers

import com.gu.pandomainauth.PublicSettings
import com.gu.typerighter.lib.PandaAuthentication
import play.api.mvc._
import services._
import scala.concurrent.ExecutionContext

/** The controller that handles the management of matcher rules.
  */
class RulesController(
    cc: ControllerComponents,
    matcherPool: MatcherPool,
    sheetId: String,
    val publicSettings: PublicSettings,
    val ruleManagerUrl: String
)(implicit ec: ExecutionContext)
    extends AbstractController(cc)
    with PandaAuthentication {

  def rules = ApiAuthAction { implicit request: Request[AnyContent] =>

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
