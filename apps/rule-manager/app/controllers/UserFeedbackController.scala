package controllers

import com.gu.typerighter.controllers.PandaAuthController
import db.{UserFeedback => DbUserFeedback}
import models.{UserFeedback, UserFeedbackWithAuthentication}
import play.api.libs.json.Json
import play.api.mvc._
import utils.{FormHelpers, RuleManagerConfig}

import scala.util.{Failure, Success}

class UserFeedbackController(
    controllerComponents: ControllerComponents,
    val config: RuleManagerConfig
) extends PandaAuthController(controllerComponents, config)
    with FormHelpers {

  def create = APIAuthAction { implicit request =>
    UserFeedback.form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          BadRequest(Json.toJson(formWithErrors.errors))
        },
        userFeedback => {
          val feedbackWithAuth =
            UserFeedbackWithAuthentication.fromUserFeedback(userFeedback, request.user.email)
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
}
