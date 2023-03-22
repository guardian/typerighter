package controllers

import com.gu.pandomainauth.PublicSettings
import com.gu.typerighter.lib.PandaAuthentication
import com.gu.typerighter.rules.{BucketRuleManager, SheetsRuleManager}
import play.api.libs.json.Json
import play.api.mvc._
import service.DbRuleManager

import scala.concurrent.ExecutionContext

/** The controller that handles the management of matcher rules.
  */
class RulesController(
    cc: ControllerComponents,
    sheetsRuleManager: SheetsRuleManager,
    bucketRuleManager: BucketRuleManager,
    sheetId: String,
    val publicSettings: PublicSettings
)(implicit ec: ExecutionContext)
    extends AbstractController(cc)
    with PandaAuthentication {
  def refresh = ApiAuthAction { implicit request: Request[AnyContent] =>
    sheetsRuleManager.getRules().flatMap { sheetRules =>
      // write rules to DB and read rules back from DB
      val dbRules = DbRuleManager.overwriteAllRules(sheetRules)

      val maybeRules = bucketRuleManager.putRules(dbRules).flatMap { _ =>
        bucketRuleManager.getRulesLastModified.map { lastModified =>
          (dbRules, lastModified)
        }
      }
      maybeRules.left.map { error => List(error.getMessage) }
    } match {
      case Right((ruleResource, _)) =>
        Ok(Json.toJson(ruleResource))
      case Left(errors) =>
        InternalServerError(Json.toJson(errors))
    }
  }

  def rules = ApiAuthAction { implicit request: Request[AnyContent] =>
    bucketRuleManager.getRules() match {
      case Right((ruleResource, _)) =>
        Ok(Json.toJson(ruleResource))
      case Left(error) =>
        InternalServerError(Json.toJson(error.getMessage))
    }
  }
}
