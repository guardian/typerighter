package controllers

import model.{Category, Rule}
import play.api.mvc._
import rules.RuleResource
import services._

import scala.concurrent.{ExecutionContext}

/**
 * The controller that handles the management of matcher rules.
 */
class RulesController(cc: ControllerComponents, matcherPool: MatcherPool, languageToolFactory: LanguageToolFactory, ruleResource: RuleResource, sheetId: String)(implicit ec: ExecutionContext)  extends AbstractController(cc) {
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

  private def addMatcherToPool(rulesByCategory: Map[Category, List[Rule]]) = {
    rulesByCategory.map { case (category, rules) => {
      val (matcher, errors) = languageToolFactory.createInstance(category.name, MatcherConfig(rules))
      matcherPool.addMatcher(category, matcher)
      (category.name, errors)
    }}.toList
  }
}
