import java.io.File

import controllers.ApiController
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.mvc.EssentialFilter
import play.filters.HttpFiltersComponents
import play.filters.cors.CORSComponents
import router.Routes
import services.{LanguageToolFactory, RuleManager, ValidatorPool}
import utils.Loggable

class AppComponents(context: Context)
  extends BuiltInComponentsFromContext(context)
  with HttpFiltersComponents
  with CORSComponents
  with Loggable {

  override def httpFilters: Seq[EssentialFilter] = corsFilter +: super.httpFilters.filterNot(allowedHostsFilter ==)

  val ngramPath: Option[File] = configuration.getOptional[String]("typerighter.ngramPath").map(new File(_))
  val languageToolFactory = new LanguageToolFactory(ngramPath)
  val validatorPool = new ValidatorPool(languageToolFactory)

  val ruleManager = new RuleManager(configuration)
  val apiController = new ApiController(controllerComponents, validatorPool, ruleManager, configuration)

  lazy val router = new Routes(
    httpErrorHandler,
    apiController
  )
}