import java.io.File

import controllers.{ApiController, RuleController}
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.mvc.EssentialFilter
import play.filters.HttpFiltersComponents
import play.filters.cors.CORSComponents
import router.Routes
import services.{LanguageTool, RuleManager}
import utils.Loggable

class AppComponents(context: Context)
  extends BuiltInComponentsFromContext(context)
  with HttpFiltersComponents
  with CORSComponents
  with Loggable {

  override def httpFilters: Seq[EssentialFilter] = corsFilter +: super.httpFilters.filterNot(allowedHostsFilter ==)

  logger.info(s"Starting with ${httpFilters.size} filters")

  val ngramPath: Option[File] = configuration.getOptional[String]("typerighter.ngramPath").map(new File(_))
  val languageTool: LanguageTool = LanguageTool.createInstance(ngramPath)

  val ruleManager = new RuleManager(configuration)
  val apiController = new ApiController(controllerComponents, languageTool, ruleManager, configuration)
  val ruleController = new RuleController(controllerComponents, languageTool, ruleManager)

  lazy val router = new Routes(
    httpErrorHandler,
    apiController,
    ruleController
  )
}