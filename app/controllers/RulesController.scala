package controllers

import model.{Category, Rule}
import play.api.Configuration
import play.api.mvc._
import rules.RuleResource
import services._

import scala.concurrent.{ExecutionContext, Future}

/**
 * The controller that handles the management of validator rules.
 */
class RulesController(cc: ControllerComponents, validatorPool: ValidatorPool, languageToolFactory: LanguageToolFactory, ruleResource: RuleResource, sheetId: String)(implicit ec: ExecutionContext)  extends AbstractController(cc) {
  def refresh = Action.async { implicit request: Request[AnyContent] =>
    for {
      (rulesByCategory, ruleErrors) <- ruleResource.fetchRulesByCategory()
      errorsByCategory = addValidatorToPool(rulesByCategory)
      rules <- validatorPool.getCurrentRules
    } yield {
      val errors = errorsByCategory.map(_._2).flatten ::: ruleErrors
      val rulesIngested = rulesByCategory.map { _._2.size }.sum
      Ok(views.html.rules(sheetId, rules, Some(rulesIngested), errors))
    }
  }

  def rules = Action.async { implicit request: Request[AnyContent] =>
    validatorPool.getCurrentRules.map { rules =>
      Ok(views.html.rules(sheetId, rules))
    }
  }

  private def addValidatorToPool(rulesByCategory: Map[Category, List[Rule]]) = {
    rulesByCategory.map { case (category, rules) => {
      val (validator, errors) = languageToolFactory.createInstance(category.name, ValidatorConfig(rules))
      validatorPool.addValidator(category.name, validator)
      (category.name, errors)
    }}.toList
  }
}
