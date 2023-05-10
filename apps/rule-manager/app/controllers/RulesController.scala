package controllers

import com.gu.pandomainauth.PublicSettings
import com.gu.permissions.PermissionDefinition
import com.gu.typerighter.lib.PandaAuthentication
import com.gu.typerighter.rules.BucketRuleResource

import play.api.libs.json.Json
import play.api.data.FormError
import play.api.libs.json.{JsValue, Writes}
import play.api.mvc._

import db.DbRuleDraft
import model.{CreateRuleForm, UpdateRuleForm}
import utils.{PermissionsHandler, RuleManagerConfig}
import service.{RuleManagement, SheetsRuleManager}

import scala.util.{Failure, Success}

/** The controller that handles the management of matcher rules.
  */
class RulesController(
    cc: ControllerComponents,
    sheetsRuleManager: SheetsRuleManager,
    bucketRuleResource: BucketRuleResource,
    val publicSettings: PublicSettings,
    override val config: RuleManagerConfig
) extends AbstractController(cc)
    with PandaAuthentication
    with PermissionsHandler {
  def refresh = ApiAuthAction {
    val maybeWrittenRules = for {
      dbRules <- sheetsRuleManager.getRules()
      _ <- RuleManagement.destructivelyPublishRules(dbRules, bucketRuleResource)
    } yield {
      RuleManagement.getDraftRules()
    }

    maybeWrittenRules match {
      case Right(ruleResource) => Ok(Json.toJson(ruleResource))
      case Left(errors)        => InternalServerError(Json.toJson(errors))
    }
  }

  def rules = ApiAuthAction {
    Ok(Json.toJson(RuleManagement.getDraftRules()))
  }

  def rule(id: Int) = ApiAuthAction {
    DbRuleDraft.find(id) match {
      case None         => NotFound("Rule not found matching ID")
      case Some(result) => Ok(Json.toJson(result))
    }
  }

  implicit object FormErrorWrites extends Writes[FormError] {
    override def writes(o: FormError): JsValue = Json.obj(
      "key" -> Json.toJson(o.key),
      "message" -> Json.toJson(o.message)
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
}
