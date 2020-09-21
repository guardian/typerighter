package controllers

import matchers.{LanguageToolFactory, RegexMatcher}
import com.gu.pandomainauth.PublicSettings
import model.{Category, RegexRule}
import play.api.mvc._
import rules.{BucketRuleResource, SheetsRuleResource}
import services._

import scala.concurrent.ExecutionContext

/**
 * The controller that handles the management of matcher rules.
 */
class RulesController(cc: ControllerComponents, matcherPool: MatcherPool, ruleResource: SheetsRuleResource, ruleBucketResouce: BucketRuleResource, sheetId: String, val publicSettings: PublicSettings)(implicit ec: ExecutionContext)
  extends AbstractController(cc) with PandaAuthentication {
  def refresh = ApiAuthAction.async { implicit request: Request[AnyContent] =>

    // This reset will need to be revisited when we're ingesting from multiple matchers.
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
              matcherPool.removeAllMatchers()
              val rulesByCategory = rules.groupBy(_.category)
              val errorsByCategory = addMatcherToPool(rulesByCategory).flatMap(_._2)
              val rulesIngested = rules.length
              Ok(views.html.rules(
                sheetId,
                matcherPool.getCurrentRules,
                matcherPool.getCurrentCategories,
                Some(true),
                Some(rulesIngested),
                errorsByCategory
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


  private def addMatcherToPool(rulesByCategory: Map[Category, List[RegexRule]]) = {
    rulesByCategory.map { case (category, rules) => {
      val validator = new RegexMatcher(category.name, rules)
      matcherPool.addMatcher(category, validator)
      (category.name, List.empty)
    }}.toList
  }
}
