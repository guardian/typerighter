import play.api.ApplicationLoader.Context
import router.Routes
import controllers.HomeController
import play.filters.HttpFiltersComponents
import play.api.BuiltInComponentsFromContext
import play.api.http.PreferredMediaTypeHttpErrorHandler
import play.api.http.JsonHttpErrorHandler
import play.api.http.DefaultHttpErrorHandler
import controllers.AssetsComponents
import db.RuleManagerDB

class AppComponents(context: Context) extends BuiltInComponentsFromContext(context) with HttpFiltersComponents with AssetsComponents {
  val dbUrl = configuration.get[String]("db.default.url")
  val dbUsername = configuration.get[String]("db.default.username")
  val dbPassword = configuration.get[String]("db.default.password")
  val db = new RuleManagerDB(dbUrl, dbUsername, dbPassword)

  val homeController = new HomeController(controllerComponents, db)

  lazy val router = new Routes(
    httpErrorHandler,
    assets,
    homeController
  )
}
