package controllers

import com.gu.pandomainauth.PublicSettings
import com.gu.typerighter.lib.PandaAuthentication
import com.gu.typerighter.rules.BucketRuleResource
import play.api.libs.json.Json
import db.DbRuleDraft
import model.{CreateRuleForm, PublishRuleForm, UpdateRuleForm}
import play.api.mvc._
import service.{RuleManager, SheetsRuleResource}
import utils.FormHelpers

import scala.util.{Failure, Success}

/** The controller that handles the management of matcher rules.
  */
class RulesController(
    cc: ControllerComponents,
    sheetsRuleResource: SheetsRuleResource,
    bucketRuleResource: BucketRuleResource,
    val publicSettings: PublicSettings
) extends AbstractController(cc)
    with PandaAuthentication
    with FormHelpers {
  def refresh = ApiAuthAction {
    val maybeWrittenRules = for {
      dbRules <- sheetsRuleResource.getRules()
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
    DbRuleDraft.find(id) match {
      case None         => NotFound("Rule not found matching ID")
      case Some(result) => Ok(Json.toJson(result))
    }
  }

  def publish(id: Int) = ApiAuthAction { implicit request =>
    PublishRuleForm.form
      .bindFromRequest()
      .fold(
        form => BadRequest(Json.toJson(form.errors)),
        reason =>
          RuleManager
            .publishRule(id, request.user.email, reason, bucketRuleResource) match {
            case Success(result) => Ok(Json.toJson(result))
            case Failure(error)  => BadRequest(error.getMessage)
          }
      )
  }

  def create = ApiAuthAction { implicit request =>
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

  def update(id: Int) = ApiAuthAction { implicit request =>
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
