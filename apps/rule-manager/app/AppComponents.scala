
import play.api.ApplicationLoader.Context
import play.filters.HttpFiltersComponents
import play.api.BuiltInComponentsFromContext
import play.api.http.PreferredMediaTypeHttpErrorHandler
import play.api.http.JsonHttpErrorHandler
import play.api.http.DefaultHttpErrorHandler
import controllers.AssetsComponents
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.auth.AWSCredentialsProvider
import com.gu.pandomainauth.PanDomainAuthSettingsRefresher
import com.gu.AwsIdentity
import com.gu.AppIdentity
import com.gu.DevIdentity

import router.Routes
import controllers.HomeController
import db.RuleManagerDB
import play.api.libs.ws.ahc.AhcWSComponents

class AppComponents(context: Context, identity: AppIdentity, creds: AWSCredentialsProvider)
  extends BuiltInComponentsFromContext(context)
  with HttpFiltersComponents
  with AssetsComponents
  with AhcWSComponents {
  val dbUrl = configuration.get[String]("db.default.url")
  val dbUsername = configuration.get[String]("db.default.username")
  val dbPassword = configuration.get[String]("db.default.password")
  val db = new RuleManagerDB(dbUrl, dbUsername, dbPassword)

  private val s3Client = AmazonS3ClientBuilder.standard().withCredentials(creds).withRegion(AppIdentity.region).build()
  val stageDomain = identity match {
    case identity: AwsIdentity if identity.stage == "PROD" => "gutools.co.uk"
    case identity: AwsIdentity => s"${identity.stage.toLowerCase}.dev-gutools.co.uk"
    case _: DevIdentity => "local.dev-gutools.co.uk"
  }
  val appName = identity match {
    case identity: AwsIdentity => identity.app
    case identity: DevIdentity => identity.app
  }

  val panDomainSettings = new PanDomainAuthSettingsRefresher(
    domain = stageDomain,
    system = appName,
    bucketName = "pan-domain-auth-settings",
    settingsFileKey = s"$stageDomain.settings",
    s3Client = s3Client
  )

  val homeController = new HomeController(
    controllerComponents,
    db,
    panDomainSettings,
    wsClient,
    stageDomain
  )

  lazy val router = new Routes(
    httpErrorHandler,
    assets,
    homeController
  )
}
