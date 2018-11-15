import java.io.File

import controllers.ApiController
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.mvc.EssentialFilter
import play.filters.HttpFiltersComponents
import play.filters.cors.CORSComponents
import router.Routes
import services.LanguageTool
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

  val apiController = new ApiController(controllerComponents, languageTool)

  lazy val router = new Routes(
    httpErrorHandler,
    apiController
  )
}