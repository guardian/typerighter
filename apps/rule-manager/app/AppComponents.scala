import play.api.ApplicationLoader.Context
import play.filters.HttpFiltersComponents
import play.api.BuiltInComponentsFromContext
import play.api.http.PreferredMediaTypeHttpErrorHandler
import play.api.http.JsonHttpErrorHandler
import play.api.http.DefaultHttpErrorHandler
import play.api.libs.ws.ahc.AhcWSComponents
import controllers.{AssetsComponents, HomeController, RulesController}
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.auth.AWSCredentialsProvider
import com.gu.pandomainauth.{PanDomainAuthSettingsRefresher, PublicSettings}
import com.gu.AwsIdentity
import com.gu.AppIdentity
import com.gu.DevIdentity
import com.gu.typerighter.rules.{BucketRuleManager, SheetsRuleManager}
import router.Routes
import db.RuleManagerDB
import play.api.db.evolutions.EvolutionsComponents
import play.api.db.{DBComponents, HikariCPComponents}
import utils.RuleManagerConfig

class AppComponents(
    context: Context,
    region: String,
    identity: AppIdentity,
    creds: AWSCredentialsProvider
) extends BuiltInComponentsFromContext(context)
    with HttpFiltersComponents
    with AssetsComponents
    with DBComponents
    with HikariCPComponents
    with EvolutionsComponents
    with AhcWSComponents {
  val config = new RuleManagerConfig(configuration, region, identity, creds)
  val db = new RuleManagerDB(config.dbUrl, config.dbUsername, config.dbPassword)

  applicationEvolutions

  private val s3Client = AmazonS3ClientBuilder
    .standard()
    .withCredentials(creds)
    .withRegion(region)
    .build()

  val stageDomain = identity match {
    case identity: AwsIdentity if identity.stage == "PROD" => "gutools.co.uk"
    case identity: AwsIdentity => s"${identity.stage.toLowerCase}.dev-gutools.co.uk"
    case _: DevIdentity        => "local.dev-gutools.co.uk"
  }
  val appName = identity match {
    case identity: AwsIdentity => identity.app
    case identity: DevIdentity => identity.app
  }

  val publicSettingsFile = identity match {
    case identity: AwsIdentity if identity.stage == "PROD" => "gutools.co.uk.settings.public"
    case identity: AwsIdentity => s"${identity.stage.toLowerCase}.dev-gutools.co.uk.settings.public"
    case _: DevIdentity        => "local.dev-gutools.co.uk.settings.public"
  }
  val publicSettings = new PublicSettings(publicSettingsFile, "pan-domain-auth-settings", s3Client)
  publicSettings.start()

  val panDomainSettings = new PanDomainAuthSettingsRefresher(
    domain = stageDomain,
    system = appName,
    bucketName = "pan-domain-auth-settings",
    settingsFileKey = s"$stageDomain.settings",
    s3Client = s3Client
  )

  val stage = identity match {
    case identity: AwsIdentity => identity.stage.toLowerCase
    case _                     => "code"
  }
  val typerighterBucket = s"typerighter-app-${stage}"

  val sheetsRuleManager = new SheetsRuleManager(config.credentials, config.spreadsheetId)
  val bucketRuleManager = new BucketRuleManager(s3Client, typerighterBucket, stage)

  val homeController = new HomeController(
    controllerComponents,
    db,
    panDomainSettings,
    wsClient,
    config
  )

  val rulesController = new RulesController(
    controllerComponents,
    sheetsRuleManager,
    bucketRuleManager,
    config.spreadsheetId,
    publicSettings
  )

  lazy val router = new Routes(
    httpErrorHandler,
    assets,
    homeController,
    rulesController
  )
}
