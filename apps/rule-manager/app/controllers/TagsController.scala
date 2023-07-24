package controllers

import com.gu.permissions.PermissionDefinition
import com.gu.typerighter.controllers.PandaAuthController
import model.TagForm
import play.api.libs.json.Json
import play.api.mvc._
import utils.{DbException, FormHelpers, NotFoundException, PermissionsHandler, RuleManagerConfig}

import scala.util.{Failure, Success}
import db.{RuleTagDraft, RuleTagLive, Tags}
import db.Tags.format

class TagsController(
    controllerComponents: ControllerComponents,
    override val config: RuleManagerConfig
) extends PandaAuthController(controllerComponents, config)
    with PermissionsHandler
    with FormHelpers {

  def listWithRuleCounts = APIAuthAction {
    val tagsWithRuleCounts = Tags.findAllWithRuleCounts()

    val json = tagsWithRuleCounts.map(tagWithRuleCount =>
      Json.obj(
        "id" -> tagWithRuleCount.id.get,
        "name" -> tagWithRuleCount.name,
        "ruleCount" -> tagWithRuleCount.ruleCount
      )
    )

    Ok(Json.toJson(json))
  }

  def get(id: Int) = APIAuthAction {
    Tags.find(id) match {
      case None => NotFound("Tag not found matching ID")
      case Some(tag) =>
        Ok(Json.toJson(tag))
    }
  }

  def delete(id: Int) = APIAuthAction {
    Tags.find(id) match {
      case None => NotFound("Tag not found matching ID")
      case Some(tag) =>
        RuleTagDraft.destroyForTag(tag.id.get)
        RuleTagLive.destroyForTag(tag.id.get)
        Tags.destroy(tag)
        Ok(s"Tag with id ${tag.id.get} deleted.")
    }
  }

  def create = APIAuthAction { implicit request =>
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

  def update(id: Int) = APIAuthAction { implicit request =>
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
                case Left(e: Exception)               => InternalServerError(e.getMessage)
                case Right(tag)                       => Ok(Json.toJson(tag))
              }
            }
          )
    }
  }
}
