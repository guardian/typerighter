package controllers

import model.CheckQuery
import play.api.Logger
import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.mvc._
import services.{LanguageTool, RuleManager}
import play.api.{Configuration}

import scala.concurrent.ExecutionContext

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
class ApiController (cc: ControllerComponents, lt: LanguageTool, ruleManager: RuleManager, configuration: Configuration)(implicit ec: ExecutionContext)  extends AbstractController(cc) {
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok("{}")
  }

  def healthcheck() = Action { implicit request: Request[AnyContent] =>
    Ok("""{ "healthy" : "true" }""")
  }

  def check: Action[JsValue] = Action(parse.json) { request =>
    val result: JsResult[Result] = for {
      checkQuery <- request.body.validate[CheckQuery]
    } yield {
      val results = lt.check(checkQuery.text)
      val json = Json.obj(
        "input" -> checkQuery.text,
        "results" -> Json.toJson(results)
      )
      Ok(json)
    }
    result.fold(
      error => BadRequest(s"Invalid request: $error"),
      identity
    )
  }

  def refresh = Action.async { implicit request: Request[AnyContent] =>
    ruleManager.getAll().map { rules =>
      Logger.info("Adding rules")
      rules.foreach(rule => Logger.info(rule.toString))
      lt.reingestRules(rules)
      Ok(s"Rules successfully ingested from spreadsheet ${configuration.getOptional[String]("typerighter.sheetId").get} -- ${rules.size} rules added.")
    }
  }
}
