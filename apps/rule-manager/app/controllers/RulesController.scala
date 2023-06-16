package controllers

import com.gu.pandomainauth.PublicSettings
import com.gu.permissions.PermissionDefinition
import com.gu.typerighter.lib.PandaAuthentication
import com.gu.typerighter.rules.BucketRuleResource
import play.api.libs.json.{JsSuccess, JsValue, Json, Reads}
import db.DbRuleDraft
import model.{CreateRuleForm, PublishRuleForm, UpdateRuleForm}
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
      case Some((draftRule, liveRules)) =>
        Ok(
          Json.obj(
            "draft" -> Json.toJson(draftRule),
            "live" -> Json.toJson(liveRules)
          )
        )
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

  def batchUpdate() = ApiAuthAction { implicit request =>
    hasPermission(request.user, PermissionDefinition("manage_rules", "typerighter")) match {
      case false => Unauthorized("You don't have permission to edit rules")
      case true =>
        val jsonBody = request.body.asJson
        jsonBody match {
          case Some(json) =>
            val validatedRequest = json.validate[BatchUpdateRequest]
            validatedRequest.fold(
              errors => {
                val errorMessages = errors.flatMap { case (path, validationErrors) =>
                  validationErrors.map(error => s"${path.toString}: ${error.message}")
                }
                BadRequest(
                  Json.obj(
                    "errors"
                      -> errorMessages
                  )
                )
              },
              batchUpdateRequest => {
                val ids = batchUpdateRequest.ids
                val updatedRuleForm = UpdateRuleForm(
                  category = batchUpdateRequest.fields.category,
                  tags = batchUpdateRequest.fields.tags
                )
                val filledForm = UpdateRuleForm.form.fill(updatedRuleForm)

                filledForm
                  .fold(
                    formWithErrors => {
                      val errors = formWithErrors.errors
                      BadRequest(Json.toJson(errors))
                    },
                    formRule => {
                      DbRuleDraft.batchUpdateFromFormRule(formRule, ids, request.user.email) match {
                        case Success(dbRules) => Ok(Json.toJson(dbRules))
                        case Failure(error)   => InternalServerError(error.getMessage())
                      }
                    }
                  )
              }
            )
          case None => BadRequest("No JSON body found")
        }
    }
  }

  case class BatchUpdateRequest(ids: List[Int], fields: BatchUpdateFields)

  case class BatchUpdateFields(category: Option[String], tags: Option[String])

  object BatchUpdateRequest {
    implicit val batchUpdateFieldsReads: Reads[BatchUpdateFields] = Reads { json =>
      val category = (json \ "category").asOpt[String]
      val tags = (json \ "tags").asOpt[String]

      val batchUpdateFields = BatchUpdateFields(category, tags)

      JsSuccess(batchUpdateFields)
    }
    implicit val batchUpdateRequestReads: Reads[BatchUpdateRequest] = Reads { json =>
      val ids = (json \ "ids").as[List[Int]]
      val fieldsJson = (json \ "fields").as[JsValue]
      val fields = fieldsJson.asOpt[BatchUpdateFields].getOrElse(BatchUpdateFields(None, None))

      JsSuccess(BatchUpdateRequest(ids, fields))
    }
  }
}
