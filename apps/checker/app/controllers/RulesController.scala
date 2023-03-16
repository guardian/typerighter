package controllers

import com.gu.pandomainauth.PublicSettings
import com.gu.typerighter.lib.PandaAuthentication
import com.gu.typerighter.rules.{BucketRuleManager, SheetsRuleManager}
import play.api.mvc._
import services._

import scala.concurrent.ExecutionContext

/** The controller that handles the management of matcher rules.
  */
class RulesController(
    cc: ControllerComponents,
    matcherPool: MatcherPool,
    sheetsRuleManager: SheetsRuleManager,
    bucketRuleManager: BucketRuleManager,
    sheetId: String,
    ruleProvisioner: RuleProvisionerService,
    val publicSettings: PublicSettings
)(implicit ec: ExecutionContext)
    extends AbstractController(cc)
    with PandaAuthentication {
  def refresh = ApiAuthAction { implicit request: Request[AnyContent] =>
    sheetsRuleManager.getRules().flatMap { ruleResource =>
      val maybeRules = bucketRuleManager.putRules(ruleResource).flatMap { _ =>
        bucketRuleManager.getRulesLastModified.map { lastModified =>
          (ruleResource, lastModified)
        }
      }
      maybeRules.left.map { error => List(error.getMessage) }
    } match {
      case Right((ruleResource, lastModified)) =>
        val ruleErrors = ruleProvisioner.updateRules(ruleResource, lastModified) match {
          case Right(())    => Nil
          case Left(errors) => errors.map(_.getMessage)
        }
        val currentRules = matcherPool.getCurrentRules
        Ok(
          views.html.rules(
            sheetId,
            currentRules,
            matcherPool.getCurrentMatchers,
            Some(true),
            Some(currentRules.size),
            ruleErrors
          )
        )
      case Left(errors) =>
        Ok(
          views.html.rules(
            sheetId,
            matcherPool.getCurrentRules,
            matcherPool.getCurrentMatchers,
            Some(false),
            Some(0),
            errors
          )
        )
    }
  }

  def rules = ApiAuthAction { implicit request: Request[AnyContent] =>
    Ok(
      views.html.rules(
        sheetId,
        matcherPool.getCurrentRules,
        matcherPool.getCurrentMatchers
      )
    )
  }
}
