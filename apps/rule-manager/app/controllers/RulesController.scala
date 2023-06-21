package controllers

import com.gu.permissions.PermissionDefinition
import com.gu.typerighter.controllers.PandaAuthController
import com.gu.typerighter.model.Document
import com.gu.typerighter.rules.BucketRuleResource
import play.api.libs.json.{JsValue, Json}
import db.DbRuleDraft
import model.{CreateRuleForm, PublishRuleForm, UpdateRuleForm}
import play.api.mvc._
import service.{RuleManager, RuleTesting, SheetsRuleResource}
import utils.{FormErrorEnvelope, FormHelpers, PermissionsHandler, RuleManagerConfig}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/** The controller that handles the management of matcher rules.
  */
class RulesController(
    controllerComponents: ControllerComponents,
    sheetsRuleResource: SheetsRuleResource,
    bucketRuleResource: BucketRuleResource,
    ruleTesting: RuleTesting,
    val config: RuleManagerConfig
)(implicit ec: ExecutionContext)
    extends PandaAuthController(controllerComponents, config)
    with PermissionsHandler
    with FormHelpers {
  def refresh = APIAuthAction {
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

  def list = APIAuthAction {
    Ok(Json.toJson(RuleManager.getDraftRules()))
  }

  def get(id: Int) = APIAuthAction {
    RuleManager.getAllRuleData(id) match {
      case None => NotFound("Rule not found matching ID")
      case Some((draftRule, liveRules)) =>
        Ok(
          Json.obj(
            "draft" -> Json.toJson(draftRule),
            "live" -> Json.toJson(liveRules)
          )
        )
    }
  }

  def publish(id: Int) = APIAuthAction { implicit request =>
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

  def create = APIAuthAction { implicit request =>
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

  def update(id: Int) = APIAuthAction { implicit request =>
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

  def test(id: Int) = APIAuthAction[JsValue](parse.json).async { implicit request =>
    request.body.validate[Document].asEither match {
      case Right(document) =>
        DbRuleDraft.find(id).flatMap { draftRule =>
          val liveRule = draftRule.toLive("placeholder")
          RuleManager.liveDbRuleToCheckerRule(liveRule).toOption
        } match {
          case Some(rule) =>
            ruleTesting
              .testRule(rule, List(document))
              .map { resultStream =>
                Ok.chunked(resultStream.bodyAsSource)
              }
          case None => Future.successful(NotFound(s"No rule with ID: ${id}"))
        }

      case Left(error) => Future.successful(BadRequest(s"Invalid request: $error"))
    }
  }
}
