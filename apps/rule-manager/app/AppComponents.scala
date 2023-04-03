import play.api.ApplicationLoader.Context
import play.filters.HttpFiltersComponents
import play.api.BuiltInComponentsFromContext
import play.api.http.PreferredMediaTypeHttpErrorHandler
import play.api.http.JsonHttpErrorHandler
import play.api.http.DefaultHttpErrorHandler
import play.api.libs.ws.ahc.AhcWSComponents
import controllers.{AssetsComponents, HomeController, RulesController}
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.auth.{
  AWSCredentialsProvider,
  AWSStaticCredentialsProvider,
  BasicAWSCredentials
}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.regions.Regions
import com.gu.pandomainauth.{PanDomainAuthSettingsRefresher, PublicSettings}
import com.gu.AwsIdentity
import com.gu.AppIdentity
import com.gu.DevIdentity
import com.gu.typerighter.rules.BucketRuleManager
import router.Routes
import db.DB
import play.api.db.evolutions.EvolutionsComponents
import play.api.db.{DBComponents, HikariCPComponents}
import service.SheetsRuleManager
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
  val db = new DB(config.dbUrl, config.dbUsername, config.dbPassword)

  applicationEvolutions

  private val localStackBasicAWSCredentialsProviderV1: AWSCredentialsProvider =
    new AWSStaticCredentialsProvider(new BasicAWSCredentials("accessKey", "secretKey"))

  private val standardS3Client = AmazonS3ClientBuilder
    .standard()
    .withCredentials(creds)
    .withRegion(region)
    .build()

  private val s3Client = identity match {
    case _: AwsIdentity => standardS3Client
    case _: DevIdentity =>
      AmazonS3ClientBuilder
        .standard()
        .withCredentials(localStackBasicAWSCredentialsProviderV1)
        .withEndpointConfiguration(
          new EndpointConfiguration("http://localhost:4566", Regions.EU_WEST_1.getName)
        )
        // This is needed for localstack
        .enablePathStyleAccess()
        .build()
  }

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
  val publicSettings =
    new PublicSettings(publicSettingsFile, "pan-domain-auth-settings", standardS3Client)
  publicSettings.start()

  val panDomainSettings = new PanDomainAuthSettingsRefresher(
    domain = stageDomain,
    system = appName,
    bucketName = "pan-domain-auth-settings",
    settingsFileKey = s"$stageDomain.settings",
    s3Client = standardS3Client
  )

  val stage = identity match {
    case identity: AwsIdentity => identity.stage.toLowerCase
    case _: DevIdentity        => "local"
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
    publicSettings
  )

  lazy val router = new Routes(
    httpErrorHandler,
    assets,
    homeController,
    rulesController
  )
}
