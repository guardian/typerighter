package controllers

import matchers.{LanguageToolFactory, RegexMatcher}
import com.gu.pandomainauth.PublicSettings
import model.{Category, RegexRule}
import play.api.mvc._
import rules.SheetsRuleResource
import services._

import scala.concurrent.ExecutionContext

/**
 * The controller that handles the management of matcher rules.
 */
class RulesController(cc: ControllerComponents, matcherPool: MatcherPool, ruleResource: SheetsRuleResource, sheetId: String, val publicSettings: PublicSettings)(implicit ec: ExecutionContext)
  extends AbstractController(cc) with PandaAuthentication {
  def refresh = ApiAuthAction.async { implicit request: Request[AnyContent] =>

    // This reset will need to be revisited when we're ingesting from multiple matchers.
    matcherPool.removeAllMatchers()

    ruleResource.fetchRulesByCategory().map { maybeRules =>
      maybeRules match {
        case Left(errors) => {
          Ok(views.html.rules(
            sheetId,
            matcherPool.getCurrentRules,
            matcherPool.getCurrentCategories,
            None,
            errors
          ))
        }
        case Right(rules) => {
          val errorsByCategory = addMatcherToPool(rules).flatMap(_._2)
          val rulesIngested = rules.map { _._2.size }.sum
          Ok(views.html.rules(
            sheetId,
            matcherPool.getCurrentRules,
            matcherPool.getCurrentCategories,
            Some(rulesIngested),
            errorsByCategory
          ))
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


  private def addMatcherToPool(rulesByCategory: Map[Category, List[RegexRule]]) = {
    rulesByCategory.map { case (category, rules) => {
      val validator = new RegexMatcher(category.name, rules)
      matcherPool.addMatcher(category, validator)
      (category.name, List.empty)
    }}.toList
  }
}
