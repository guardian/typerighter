import java.io.File

import controllers.{ApiController, RuleController}
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.mvc.EssentialFilter
import play.filters.HttpFiltersComponents
import play.filters.cors.CORSComponents
import router.Routes
import services.{LanguageToolInstancePool, LanguageToolFactory}
import utils.Loggable

class AppComponents(context: Context)
  extends BuiltInComponentsFromContext(context)
  with HttpFiltersComponents
  with CORSComponents
  with Loggable {

  override def httpFilters: Seq[EssentialFilter] = corsFilter +: super.httpFilters.filterNot(allowedHostsFilter ==)

  logger.info(s"Starting with ${httpFilters.size} filters")

  val ngramPath: Option[File] = configuration.getOptional[String]("typerighter.ngramPath").map(new File(_))
  val languageToolFactory = new LanguageToolFactory( "all-categories", ngramPath)
  val languageToolCategoryHandler = new LanguageToolInstancePool(languageToolFactory, 8)

  val apiController = new ApiController(controllerComponents, languageToolCategoryHandler)
  val ruleController = new RuleController(controllerComponents)

  lazy val router = new Routes(
    httpErrorHandler,
    apiController,
    ruleController
  )
}