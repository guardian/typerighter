package controllers

import com.gu.pandomainauth.PublicSettings
import com.gu.permissions.PermissionDefinition
import com.gu.typerighter.lib.PandaAuthentication
import model.TagForm
import play.api.libs.json.Json
import play.api.mvc._
import utils.{DbException, FormHelpers, NotFoundException, PermissionsHandler, RuleManagerConfig}

import scala.util.{Failure, Success}
import db.Tags
import db.Tags.format

class TagsController(
    cc: ControllerComponents,
    val publicSettings: PublicSettings,
    override val config: RuleManagerConfig
) extends AbstractController(cc)
    with PandaAuthentication
    with PermissionsHandler
    with FormHelpers {

  def list = ApiAuthAction {
    Ok(Json.toJson(Tags.findAll()))
  }

  def get(id: Int) = ApiAuthAction {
    Tags.find(id) match {
      case None => NotFound("Tag not found matching ID")
      case Some(tag) =>
        Ok(Json.toJson(tag))
    }
  }

  def delete(id: Int) = ApiAuthAction {
    Tags.find(id) match {
      case None => NotFound("Tag not found matching ID")
      case Some(tag) =>
        Tags.destroy(tag)
        Ok(s"Tag with id ${tag.id.get} deleted.")
    }
  }

  def create = ApiAuthAction { implicit request =>
    {
      hasPermission(request.user, PermissionDefinition("manage_rules", "typerighter")) match {
        case false => Unauthorized("You don't have permission to create tags")
        case true =>
          TagForm.form
            .bindFromRequest()
            .fold(
              formWithErrors => {
                val errors = formWithErrors.errors
                BadRequest(Json.toJson(errors))
              },
              formRule => {
                Tags.createFromTagForm(formRule) match {
                  case Success(tag)   => Ok(Json.toJson(tag))
                  case Failure(error) => InternalServerError(error.getMessage())
                }
              }
            )
      }
    }
  }

  def update(id: Int) = ApiAuthAction { implicit request =>
    hasPermission(request.user, PermissionDefinition("manage_rules", "typerighter")) match {
      case false => Unauthorized("You don't have permission to edit rules")
      case true =>
        TagForm.form
          .bindFromRequest()
          .fold(
            formWithErrors => {
              val errors = formWithErrors.errors
              BadRequest(Json.toJson(errors))
            },
            tagForm => {
              Tags.updateFromTagForm(id, tagForm) match {
                case Left(NotFoundException(message)) => NotFound(message)
                case Left(DbException(message))       => InternalServerError(message)
                case Right(tag)                       => Ok(Json.toJson(tag))
              }
            }
          )
    }
  }
}
