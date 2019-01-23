package controllers

import model.PatternRule
import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.mvc._
import services.{LanguageToolFactory, RuleManager}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Controller to handle CRUD operations for PatternRules.
  */
class RuleController(cc: ControllerComponents)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  def add: Action[JsValue] = Action(parse.json) { request =>
    val result = for {
      rule <- request.body.validate[PatternRule]
    } yield {
      RuleManager.add(rule)
      Ok(Json.toJson(rule))
    }
    result.fold(
      error => BadRequest(s"Invalid request: $error"),
      identity
    )
  }

  def get(id: String): Action[AnyContent] = Action(parse.json).async {
    for {
      maybeRule <- RuleManager.get(id)
    } yield {
      maybeRule match {
        case Some(rule) => Ok(Json.toJson(rule))
        case None => BadRequest(s"Rule not found for id $id")
      }
    }
  }

  def getAll = Action(parse.json) {
    // @todo
    Ok
  }
}
