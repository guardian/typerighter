package controllers

import model.{Check, ValidatorResponse}
import akka.actor.ActorSystem
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import services._
import rules.RuleResource

import scala.concurrent.{ExecutionContext, Future}

/**
  * The controller that handles API requests.
  */
class ApiController(
  cc: ControllerComponents,
  validatorPool: ValidatorPool,
  ruleResource: RuleResource
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def check: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[Check].asEither match {
      case Right(check) =>
        validatorPool
          .check(check)
          .map { matches =>
            val response = ValidatorResponse(
              matches = matches,
              blocks = check.blocks,
              categoryIds = check.categoryIds.getOrElse(validatorPool.getCurrentCategories.map { _._1 })
            )
            Ok(Json.toJson(response))
          } recover {
          case e: Exception =>
            InternalServerError(Json.obj("error" -> e.getMessage))
        }
      case Left(error) =>
        Future.successful(BadRequest(s"Invalid request: $error"))
    }
  }

  def getCurrentCategories: Action[AnyContent] = Action {
      Ok(Json.toJson(validatorPool.getCurrentCategories.map(_._2)))
  }
}
