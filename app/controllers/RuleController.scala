package controllers

import model.PatternRule
import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.mvc._
import services.{LanguageTool, RuleManager}

/**
  * Rules controller.
  */
class RuleController (cc: ControllerComponents, lt: LanguageTool) extends AbstractController(cc) {
  def add: Action[JsValue] = Action(parse.json) { request =>
    val result: JsResult[Result] = for {
      rule <- request.body.validate[PatternRule]
    } yield {
      RuleManager.add(rule)
      Ok
    }
    result.fold(
      error => BadRequest(s"Invalid request: $error"),
      identity
    )
  }

  def get(id: String): Action[AnyContent] = Action(parse.json) {
    val maybeJson = for {
      rule <- RuleManager.ruleMap.get(id)
    } yield {
     Ok(Json.toJson(rule))
    }
    maybeJson.getOrElse(BadRequest(s"Rule not found for id $id"))
  }

  def getAll = Action(parse.json) {
   Ok(Json.toJson(lt.getAllRules))
  }
}
