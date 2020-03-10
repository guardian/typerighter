package controllers

import matchers.{LanguageToolFactory, RegexMatcher}
import model.{Category, RegexRule}
import play.api.mvc._
import rules.SheetsRuleResource
import services._

import scala.concurrent.ExecutionContext

/**
 * The controller that handles the management of matcher rules.
 */
class RulesController(cc: ControllerComponents, matcherPool: MatcherPool, ruleResource: SheetsRuleResource, sheetId: String)(implicit ec: ExecutionContext)  extends AbstractController(cc) {
  def refresh = Action.async { implicit request: Request[AnyContent] =>
    for {
      (rulesByCategory, ruleErrors) <- ruleResource.fetchRulesByCategory()
      errorsByCategory = addMatcherToPool(rulesByCategory)
    } yield {
      val errors = errorsByCategory.flatMap(_._2) ::: ruleErrors
      val rulesIngested = rulesByCategory.map { _._2.size }.sum
      Ok(views.html.rules(
        sheetId,
        matcherPool.getCurrentRules,
        matcherPool.getCurrentCategories,
        Some(rulesIngested),
        errors
      ))
    }
  }

  def rules = Action { implicit request: Request[AnyContent] =>
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
