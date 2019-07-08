package controllers

import model.{Category, Rule}
import play.api.mvc._
import rules.RuleResource
import services._

import scala.concurrent.{ExecutionContext}

/**
 * The controller that handles the management of validator rules.
 */
class RulesController(cc: ControllerComponents, validatorPool: ValidatorPool, languageToolFactory: LanguageToolFactory, ruleResource: RuleResource, sheetId: String)(implicit ec: ExecutionContext)  extends AbstractController(cc) {
  def refresh = Action.async { implicit request: Request[AnyContent] =>
    for {
      (rulesByCategory, ruleErrors) <- ruleResource.fetchRulesByCategory()
      errorsByCategory = addValidatorToPool(rulesByCategory)
    } yield {
      val errors = errorsByCategory.flatMap(_._2) ::: ruleErrors
      val rulesIngested = rulesByCategory.map { _._2.size }.sum
      Ok(views.html.rules(
        sheetId,
        validatorPool.getCurrentRules,
        validatorPool.getCurrentCategories,
        Some(rulesIngested),
        errors
      ))
    }
  }

  def rules = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.rules(
      sheetId,
      validatorPool.getCurrentRules,
      validatorPool.getCurrentCategories
    ))
  }

  private def addValidatorToPool(rulesByCategory: Map[Category, List[Rule]]) = {
    rulesByCategory.map { case (category, rules) => {
      val (validator, errors) = languageToolFactory.createInstance(category.name, ValidatorConfig(rules))
      validatorPool.addValidator(category, validator)
      (category.name, errors)
    }}.toList
  }
}
