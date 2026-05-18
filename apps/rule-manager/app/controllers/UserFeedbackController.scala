package controllers

import com.gu.typerighter.controllers.PandaAuthController
import db.{UserFeedback => DbUserFeedback}
import model.UserFeedbackActionForm
import models.{UserFeedback, UserFeedbackWithEmail}
import play.api.libs.json.Json
import play.api.mvc._
import utils.{FormHelpers, RuleManagerConfig}

import scala.util.{Failure, Success}

class UserFeedbackController(
    controllerComponents: ControllerComponents,
    val config: RuleManagerConfig
) extends PandaAuthController(controllerComponents, config)
    with FormHelpers {

  def list(page: Int, queryStr: Option[String]) =
    APIAuthAction {
      Ok(Json.toJson(DbUserFeedback.searchFeedback(page, queryStr)))
    }

  def create = APIAuthAction { implicit request =>
    UserFeedback.form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          BadRequest(Json.toJson(formWithErrors.errors))
        },
        userFeedback => {
          val feedbackWithAuth =
            UserFeedbackWithEmail.fromUserFeedback(userFeedback, request.user.email)
          DbUserFeedback.create(
            feedbackWithAuth
          ) match {
            case Success(created) => Ok(Json.toJson(created))
            case Failure(e) =>
              InternalServerError(s"Failed to create user feedback: ${e.getMessage}")
          }
        }
      )
  }

  def updateAction(id: Int) = APIAuthAction(parse.json[UserFeedbackActionForm]) { request =>
    request.body match {
      case UserFeedbackActionForm(actioned, actionType, actionNotes) =>
        DbUserFeedback.updateAction(id, actioned, actionType, actionNotes) match {
          case Success(updated) => Ok(Json.toJson(updated))
          case Failure(e) =>
            InternalServerError(s"Failed to update user feedback: ${e.getMessage}")
        }
    }
  }
}
