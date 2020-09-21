package controllers

import matchers.{LanguageToolFactory, RegexMatcher}
import com.gu.pandomainauth.PublicSettings
import model.{Category, RegexRule}
import play.api.mvc._
import rules.{BucketRuleResource, RuleProvisionerService, SheetsRuleResource}
import services._

import scala.concurrent.ExecutionContext

/**
 * The controller that handles the management of matcher rules.
 */
class RulesController(cc: ControllerComponents, matcherPool: MatcherPool, ruleResource: SheetsRuleResource, ruleBucketResouce: BucketRuleResource, sheetId: String,
                      ruleProvisioner: RuleProvisionerService, val publicSettings: PublicSettings)(implicit ec: ExecutionContext)
  extends AbstractController(cc) with PandaAuthentication {
  def refresh = ApiAuthAction.async { implicit request: Request[AnyContent] =>
    ruleResource.fetchRulesByCategory().map { maybeRules =>
      maybeRules match {
        case Left(errors) => {
          Ok(views.html.rules(
            sheetId,
            matcherPool.getCurrentRules,
            matcherPool.getCurrentCategories,
            Some(false),
            None,
            errors
          ))
        }
        case Right(rules) => {
          ruleBucketResouce.serialiseAndUploadRules(rules) match {
            case Right(_) => {
              ruleProvisioner.updateRules()
              val rulesIngested = rules.length
              Ok(views.html.rules(
                sheetId,
                matcherPool.getCurrentRules,
                matcherPool.getCurrentCategories,
                Some(true),
                Some(rulesIngested)
              ))
            }
            case Left(message) => {
              Ok(views.html.rules(
                sheetId,
                matcherPool.getCurrentRules,
                matcherPool.getCurrentCategories,
                Some(false),
                None,
                List(message)
              ))
            }
          }
        }
      }
    }
  }

  def rules = ApiAuthAction { implicit request: Request[AnyContent] =>
    Ok(views.html.rules(
      sheetId,
      matcherPool.getCurrentRules,
      matcherPool.getCurrentCategories
    ))
  }
}
