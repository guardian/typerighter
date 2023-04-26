package controllers

import com.gu.pandomainauth.PublicSettings
import com.gu.typerighter.lib.PandaAuthentication
import com.gu.typerighter.rules.{BucketRuleManager, SheetsRuleManager}
import db.DbRule
import model.{CreateRuleForm, UpdateRuleForm}
import play.api.data.FormError
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc._
import service.DbRuleManager

import scala.concurrent.ExecutionContext

/** The controller that handles the management of matcher rules.
  */
class RulesController(
    cc: ControllerComponents,
    sheetsRuleManager: SheetsRuleManager,
    bucketRuleManager: BucketRuleManager,
    val publicSettings: PublicSettings
)(implicit ec: ExecutionContext)
    extends AbstractController(cc)
    with PandaAuthentication {
  def refresh = ApiAuthAction { implicit request: Request[AnyContent] =>
    val maybeWrittenRules = for {
      sheetRules <- sheetsRuleManager.getRules()
      ruleResource <- DbRuleManager.destructivelyDumpRuleResourceToDB(sheetRules)
      _ <- bucketRuleManager.putRules(ruleResource).left.map { l => List(l.toString) }
    } yield {
      DbRuleManager.getRules()
    }

    maybeWrittenRules match {
      case Right(rules) => Ok(Json.toJson(rules))
      case Left(errors) => InternalServerError(Json.toJson(errors))
    }
  }

  def rules = ApiAuthAction { implicit request: Request[AnyContent] =>
    Ok(Json.toJson(DbRuleManager.getRules()))
  }

  def tags = ApiAuthAction { implicit request: Request[AnyContent] =>
    Ok(Json.toJson(DbRuleManager.getTags()))
  }

  implicit object FormErrorWrites extends Writes[FormError] {
    override def writes(o: FormError): JsValue = Json.obj(
      "key" -> Json.toJson(o.key),
      "message" -> Json.toJson(o.message)
    )
  }

  def create = ApiAuthAction { implicit request: Request[AnyContent] =>
    CreateRuleForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val errors = formWithErrors.errors
          BadRequest(Json.toJson(errors))
        },
        formRule => {
          val dbRule = DbRule.createFromFormRule(formRule)
          Ok(DbRule.toJson(dbRule))
        }
      )
  }

  def update(id: Int) = ApiAuthAction { implicit request: Request[AnyContent] =>
    UpdateRuleForm.form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val errors = formWithErrors.errors
          BadRequest(Json.toJson(errors))
        },
        formRule => {
          DbRule.updateFromFormRule(formRule, id) match {
            case Left(result)  => result
            case Right(dbRule) => Ok(DbRule.toJson(dbRule))
          }
        }
      )
  }
}
