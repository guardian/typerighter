package controllers

import model.CheckQuery
import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.mvc._
import services._
import play.api.{Configuration, Logger}
import utils.Validator.ValidatorResponse

import scala.concurrent.{ExecutionContext, Future}

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
class ApiController(cc: ControllerComponents, validatorPool: ValidatorPool, ruleManager: RuleManager, configuration: Configuration)(implicit ec: ExecutionContext)  extends AbstractController(cc) {
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok("{}")
  }

  def healthcheck() = Action { implicit request: Request[AnyContent] =>
    Ok("""{ "healthy" : "true" }""")
  }

  def check: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[CheckQuery].asEither match {
      case Right(checkQuery) =>
        validatorPool.checkAllCategories(checkQuery.text).map(results => {
          val json = Json.obj(
            "input" -> checkQuery.text,
            "results" -> Json.toJson(results)
          )
          Ok(json)
        })
      case Left(error) => Future.successful(BadRequest(s"Invalid request: $error"))
    }
  }

  def refresh = Action.async { implicit request: Request[AnyContent] =>
    for {
      (rulesByCategory, ruleErrors) <- ruleManager.fetchByCategory()
      errorsByCategory <- Future.sequence(
        rulesByCategory.map { case (category, rules) => {
          validatorPool.updateConfig(category, ValidatorConfig(rules))
        }}.toList
      )
    } yield {
      val sheetId = configuration.getOptional[String]("typerighter.sheetId").orNull
      Ok(Json.obj(
        "sheetId" -> sheetId,
        "categories" -> rulesByCategory.map { case (category, rules) =>
          Json.obj(
            "category" -> category,
            "rulesIngested" -> rules.size
          )},
        "errors" -> Json.toJson(errorsByCategory.flatten ::: ruleErrors)
      ))
    }
  }

  def getCurrentRules = Action.async { implicit request: Request[AnyContent] =>
    validatorPool.getCurrentRules.map(rules => Ok(Json.toJson(rules)))
  }
}
