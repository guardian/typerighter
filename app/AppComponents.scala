import controllers.ApiController
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.filters.HttpFiltersComponents
import router.Routes
import services.LanguageTool

class AppComponents(context: Context)
  extends BuiltInComponentsFromContext(context)
  with HttpFiltersComponents {

  // TODO: Provide an instance
  val languageTool = LanguageTool.createInstance(None)

  val apiController = new ApiController(controllerComponents, languageTool)

  lazy val router = new Routes(
    httpErrorHandler,
    apiController
  )
}