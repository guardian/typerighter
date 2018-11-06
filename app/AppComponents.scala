import controllers.ApiController
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.mvc.EssentialFilter
import play.filters.HttpFiltersComponents
import router.Routes
import services.LanguageTool
import utils.Loggable

class AppComponents(context: Context)
  extends BuiltInComponentsFromContext(context)
  with HttpFiltersComponents
  with Loggable {

  override def httpFilters: Seq[EssentialFilter] = super.httpFilters.filterNot(allowedHostsFilter ==)

  logger.info(s"Starting with ${httpFilters.size} filters")

  // TODO: Provide an instance
  val languageTool: LanguageTool = LanguageTool.createInstance(None)

  val apiController = new ApiController(controllerComponents, languageTool)

  lazy val router = new Routes(
    httpErrorHandler,
    apiController
  )
}