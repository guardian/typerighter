package controllers

import model.CheckQuery
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import services._
import rules.RuleResource

import scala.concurrent.{ExecutionContext, Future}

/**
 * The controller that handles API requests.
 */
class ApiController(cc: ControllerComponents, validatorPool: ValidatorPool, ruleResource: RuleResource)(implicit ec: ExecutionContext)  extends AbstractController(cc) {
  def check: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[CheckQuery].asEither match {
      case Right(checkQuery) =>
        val categoryIds = checkQuery.categoryIds.getOrElse(validatorPool.getCurrentCategories.map { _.id })
        validatorPool.check(checkQuery.text, categoryIds).map(results => {
          val json = Json.obj(
            "input" -> checkQuery.text,
            "results" -> Json.toJson(results)
          )
          Ok(json)
        })
      case Left(error) => Future.successful(BadRequest(s"Invalid request: $error"))
    }
  }

  def getCurrentCategories: Action[AnyContent] = Action { request: Request[AnyContent] =>
    Ok(Json.toJson(validatorPool.getCurrentCategories))
  }
}
