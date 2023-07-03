package controllers

import com.gu.pandomainauth.PublicSettings
import com.gu.permissions.PermissionDefinition
import com.gu.typerighter.lib.PandaAuthentication
import com.gu.typerighter.rules.BucketRuleResource
import play.api.libs.json.Json
import db.DbRuleDraft
import model.{BatchUpdateRuleForm, CreateRuleForm, PublishRuleForm, UpdateRuleForm}
import play.api.mvc._
import service.{RuleManager, SheetsRuleResource}
import utils.{FormErrorEnvelope, FormHelpers, PermissionsHandler, RuleManagerConfig}

import scala.util.{Failure, Success}

/** The controller that handles the management of matcher rules.
  */
class RulesController(
    cc: ControllerComponents,
    sheetsRuleResource: SheetsRuleResource,
    bucketRuleResource: BucketRuleResource,
    val publicSettings: PublicSettings,
    override val config: RuleManagerConfig
) extends AbstractController(cc)
    with PandaAuthentication
    with PermissionsHandler
    with FormHelpers {
  def refresh = ApiAuthAction {
    val maybeWrittenRules = for {
      dbRules <- sheetsRuleResource
        .getRules()
        .left
        .map(toFormError("Error getting rules from Google Sheet"))
      _ <- RuleManager.destructivelyPublishRules(dbRules, bucketRuleResource)
    } yield {
      RuleManager.getDraftRules()
    }

    maybeWrittenRules match {
      case Right(ruleResource) => Ok(Json.toJson(ruleResource))
      case Left(errors)        => InternalServerError(Json.toJson(errors))
    }
  }

  def list = ApiAuthAction {
    Ok(Json.toJson(RuleManager.getDraftRules()))
  }

  def get(id: Int) = ApiAuthAction {
    RuleManager.getAllRuleData(id) match {
      case None => NotFound("Rule not found matching ID")
      case Some(allRuleData) =>
        Ok(Json.toJson(allRuleData))
    }
  }

  def publish(id: Int) = ApiAuthAction { implicit request =>
    PublishRuleForm.form
      .bindFromRequest()
      .fold(
        form => BadRequest(Json.toJson(FormErrorEnvelope(form.errors))),
        reason => {
          DbRuleDraft.find(id) match {
            case None => NotFound
            case _ =>
              RuleManager
                .publishRule(id, request.user.email, reason, bucketRuleResource) match {
                case Right(result) => Ok(Json.toJson(result))
                case Left(errors)  => BadRequest(Json.toJson(FormErrorEnvelope(errors)))
              }
          }
        }
      )
  }

  def create = ApiAuthAction { implicit request =>
    {
      hasPermission(request.user, PermissionDefinition("manage_rules", "typerighter")) match {
        case false => Unauthorized("You don't have permission to create rules")
        case true =>
          CreateRuleForm.form
            .bindFromRequest()
            .fold(
              formWithErrors => {
                val errors = formWithErrors.errors
                BadRequest(Json.toJson(errors))
              },
              formRule => {
                DbRuleDraft.createFromFormRule(formRule, request.user.email) match {
                  case Success(rule)  => Ok(Json.toJson(rule))
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
        UpdateRuleForm.form
          .bindFromRequest()
          .fold(
            formWithErrors => {
              val errors = formWithErrors.errors
              BadRequest(Json.toJson(errors))
            },
            formRule => {
              DbRuleDraft.updateFromFormRule(formRule, id, request.user.email) match {
                case Left(result)  => result
                case Right(dbRule) => Ok(Json.toJson(dbRule))
              }
            }
          )
    }
  }

  def getRules(ids: String) = ApiAuthAction {
    val idList = ids.split(',').map(_.toInt).toList
    val allRulesData = idList.flatMap(id => RuleManager.getAllRuleData(id))

    allRulesData match {
      case Nil => NotFound("No rules found matching IDs")
      case _   => Ok(Json.toJson(allRulesData))
    }
  }

  def batchUpdate() = ApiAuthAction { implicit request =>
    hasPermission(request.user, PermissionDefinition("manage_rules", "typerighter")) match {
      case false => Unauthorized("You don't have permission to edit rules")
      case true =>
        BatchUpdateRuleForm.form
          .bindFromRequest()
          .fold(
            formWithErrors => {
              val errors = formWithErrors.errors
              BadRequest(Json.toJson(errors))
            },
            formRule => {
              val ids = formRule.ids
              val category = formRule.fields.category
              val tags = formRule.fields.tags
              val updatedCategory = category.filter(_.nonEmpty)
              val updatedTags = tags.filter(_.nonEmpty)

              DbRuleDraft.batchUpdate(ids, updatedCategory, updatedTags, request.user.email) match {
                case Failure(e)     => InternalServerError(e.getMessage())
                case Success(rules) => Ok(Json.toJson(rules))
              }
            }
          )
    }
  }

  def canPublish(id: Int) = ApiAuthAction {
    RuleManager.parseDraftRuleForPublication(id, "validate") match {
      case Right(_)     => Ok(Json.toJson(Nil))
      case Left(errors) => Ok(Json.toJson(errors))
    }
  }

  def unpublish(id: Int): Action[AnyContent] = ApiAuthAction { implicit request =>
    hasPermission(request.user, PermissionDefinition("manage_rules", "typerighter")) match {
      case false => Unauthorized("You don't have permission to unpublish rules")
      case true =>
        RuleManager.unpublishRule(id, request.user.email, bucketRuleResource) match {
          case Left(e: Throwable) => InternalServerError(e.getMessage)
          case Right(allRuleData) => Ok(Json.toJson(allRuleData))
        }
    }
  }

  def archive(id: Int): Action[AnyContent] = ApiAuthAction { implicit request =>
    hasPermission(request.user, PermissionDefinition("manage_rules", "typerighter")) match {
      case false => Unauthorized("You don't have permission to archive rules")
      case true =>
        RuleManager.archiveRule(id, request.user.email) match {
          case Left(e: Throwable) => InternalServerError(e.getMessage)
          case Right(allRuleData) => Ok(Json.toJson(allRuleData))
        }
    }
  }

  def unarchive(id: Int): Action[AnyContent] = ApiAuthAction { implicit request =>
    hasPermission(request.user, PermissionDefinition("manage_rules", "typerighter")) match {
      case false => Unauthorized("You don't have permission to unarchive rules")
      case true =>
        RuleManager.unarchiveRule(id, request.user.email) match {
          case Left(e: Throwable) => InternalServerError(e.getMessage)
          case Right(allRuleData) => Ok(Json.toJson(allRuleData))
        }
    }
  }
}
