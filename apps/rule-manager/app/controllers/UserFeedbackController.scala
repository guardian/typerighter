package controllers

import com.gu.typerighter.controllers.PandaAuthController
import db.{UserFeedback => DbUserFeedback}
import model.UserFeedbackAddressedForm
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

  def updateAction(id: Int) = APIAuthAction(parse.json[UserFeedbackAddressedForm]) { request =>
    request.body match {
      case UserFeedbackAddressedForm(addressed, notes) =>
        DbUserFeedback.updateAction(id, addressed, notes, request.user.email) match {
          case Success(updated) => Ok(Json.toJson(updated))
          case Failure(e) =>
            InternalServerError(s"Failed to update user feedback: ${e.getMessage}")
        }
    }
  }

  def updateNotes(id: Int) = APIAuthAction(parse.json) { request =>
    val notes = (request.body \ "notes").asOpt[String]
    DbUserFeedback.updateNotes(id, notes) match {
      case Success(updated) => Ok(Json.toJson(updated))
      case Failure(e) =>
        InternalServerError(s"Failed to update user feedback notes: ${e.getMessage}")
    }
  }
}
