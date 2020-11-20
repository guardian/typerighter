import play.api.ApplicationLoader.Context
import router.Routes
import controllers.HomeController
import play.filters.HttpFiltersComponents
import play.api.BuiltInComponentsFromContext
import play.api.http.PreferredMediaTypeHttpErrorHandler
import play.api.http.JsonHttpErrorHandler
import play.api.http.DefaultHttpErrorHandler
import controllers.AssetsComponents

class AppComponents(context: Context) extends BuiltInComponentsFromContext(context) with HttpFiltersComponents with AssetsComponents {
  
  val homeController = new HomeController(controllerComponents)
  
  lazy val router = new Routes(
      httpErrorHandler,
      homeController,
      assets
  )
}