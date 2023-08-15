import com.amazonaws.auth.{
  AWSCredentialsProvider,
  AWSStaticCredentialsProvider,
  BasicAWSCredentials
}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.regions.Regions
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import com.gu.contentapi.client.GuardianContentClient
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.gu.{AppIdentity, AwsIdentity, DevIdentity}
import controllers.{
  ApiController,
  AuditController,
  CapiProxyController,
  HomeController,
  RulesController
}
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.http.{
  DefaultHttpErrorHandler,
  JsonHttpErrorHandler,
  PreferredMediaTypeHttpErrorHandler
}
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.libs.concurrent.DefaultFutures
import play.api.mvc.EssentialFilter
import play.filters.HttpFiltersComponents
import play.filters.cors.CORSComponents
import router.Routes
import services._
import com.gu.typerighter.lib.{ContentClient, Loggable}
import com.gu.typerighter.rules.BucketRuleResource
import matchers.LanguageToolFactory
import utils.CloudWatchClient
import utils.CheckerConfig

class AppComponents(
    context: Context,
    region: String,
    identity: AppIdentity,
    creds: AWSCredentialsProvider,
    credsV2: AwsCredentialsProvider
) extends BuiltInComponentsFromContext(context)
    with HttpFiltersComponents
    with CORSComponents
    with Loggable
    with controllers.AssetsComponents
    with AhcWSComponents {

  override def httpFilters: Seq[EssentialFilter] =
    corsFilter +: super.httpFilters.filterNot(allowedHostsFilter ==)

  val config = new CheckerConfig(configuration, region, identity, creds, wsClient)

  val languageToolFactory = new LanguageToolFactory(config.ngramPath, true)

  val guardianContentClient = GuardianContentClient(config.capiApiKey)
  val contentClient = new ContentClient(guardianContentClient)

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

  val settingsFile = identity match {
    case identity: AwsIdentity if identity.stage == "PROD" => "gutools.co.uk.settings.public"
    case identity: AwsIdentity => s"${identity.stage.toLowerCase}.dev-gutools.co.uk.settings.public"
    case _: DevIdentity        => "local.dev-gutools.co.uk.settings.public"
  }

  val stage = identity match {
    case identity: AwsIdentity => identity.stage.toLowerCase
    case _: DevIdentity        => "local"
  }

  val typerighterBucket = s"typerighter-app-${stage}"

  val cloudWatchClient = identity match {
    case identity: AwsIdentity => new CloudWatchClient(stage, false)
    case _: DevIdentity        => new CloudWatchClient(stage, true)
  }

  val matcherPoolDispatcher = actorSystem.dispatchers.lookup("matcher-pool-dispatcher")
  val defaultFutures = new DefaultFutures(actorSystem)
  val matcherPool = new MatcherPool(
    futures = defaultFutures,
    maybeCloudWatchClient = Some(cloudWatchClient)
  )(matcherPoolDispatcher, materializer)

  val bucketRuleResource = new BucketRuleResource(s3Client, typerighterBucket, stage)
  val matcherProvisionerService = new MatcherProvisionerService(
    bucketRuleResource,
    matcherPool,
    languageToolFactory,
    cloudWatchClient
  )

  private val ruleManagerUrl = identity match {
    case identity: AwsIdentity if identity.stage == "PROD" =>
      "https://manager.typerighter.gutools.co.uk"
    case identity: AwsIdentity =>
      s"https://manager.typerighter.${identity.stage.toLowerCase}.dev-gutools.co.uk"
    case _: DevIdentity => "https://manager.typerighter.local.dev-gutools.co.uk"
  }

  val apiController =
    new ApiController(controllerComponents, matcherPool, matcherProvisionerService, config)
  val rulesController = new RulesController(
    controllerComponents,
    matcherPool,
    config.spreadsheetId,
    ruleManagerUrl,
    config
  )
  val homeController = new HomeController(controllerComponents, matcherPool, config)
  val auditController = new AuditController(controllerComponents, config)
  val capiProxyController =
    new CapiProxyController(controllerComponents, contentClient, config)

  override lazy val httpErrorHandler = PreferredMediaTypeHttpErrorHandler(
    "application/json" -> new JsonHttpErrorHandler(environment, None),
    "text/html" -> new DefaultHttpErrorHandler()
  )

  lazy val router = new Routes(
    httpErrorHandler,
    assets,
    homeController,
    rulesController,
    auditController,
    apiController,
    capiProxyController
  )

  /** Set up matchers and add them to the matcher pool as the app starts.
    */
  matcherProvisionerService.scheduleUpdateRules(actorSystem.scheduler)
}
