package controllers

import com.gu.pandomainauth.PublicSettings
import com.gu.typerighter.lib.PandaAuthentication
import com.gu.typerighter.rules.BucketRuleManager
import play.api.libs.json.Json
import play.api.mvc._
import service.{DbRuleManager, SheetsRuleManager}

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
      dbRules <- DbRuleManager.destructivelyDumpRuleResourceToDB(sheetRules)
      _ <- bucketRuleManager.putRules(dbRules).left.map { l => List(l.toString) }
    } yield dbRules

    maybeWrittenRules match {
      case Right(ruleResource) => Ok(Json.toJson(ruleResource))
      case Left(errors)        => InternalServerError(Json.toJson(errors))
    }
  }

  def rules = ApiAuthAction { implicit request: Request[AnyContent] =>
    bucketRuleManager.getRules() match {
      case Right((ruleResource, _)) => Ok(Json.toJson(ruleResource))
      case Left(error)              => InternalServerError(Json.toJson(error.getMessage))
    }
  }
}
