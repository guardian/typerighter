import com.amazonaws.auth.{AWSCredentialsProvider}
import com.gu.contentapi.client.GuardianContentClient
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.gu.pandomainauth.PublicSettings
import com.gu.{AppIdentity, AwsIdentity, DevIdentity}
import controllers.{ApiController, AuditController, CapiProxyController, HomeController, RulesController}
import play.api.ApplicationLoader.Context
import play.api.{BuiltInComponentsFromContext}
import play.api.http.{DefaultHttpErrorHandler, JsonHttpErrorHandler, PreferredMediaTypeHttpErrorHandler}
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.libs.concurrent.DefaultFutures
import play.api.mvc.EssentialFilter
import play.filters.HttpFiltersComponents
import play.filters.cors.CORSComponents
import router.Routes
import rules.{BucketRuleManager, SheetsRuleManager}
import services._
import com.gu.typerighter.lib.{Loggable, ElkLogging}
import matchers.LanguageToolFactory
import utils.CloudWatchClient
import utils.CheckerConfig


class AppComponents(context: Context, identity: AppIdentity, creds: AWSCredentialsProvider)
  extends BuiltInComponentsFromContext(context)
  with HttpFiltersComponents
  with CORSComponents
  with Loggable
  with controllers.AssetsComponents
  with AhcWSComponents {

  override def httpFilters: Seq[EssentialFilter] = corsFilter +: super.httpFilters.filterNot(allowedHostsFilter ==)

  val config = new CheckerConfig(configuration, identity, creds)

  // initialise log shipping if we are in AWS
  private val logShipping = Some(identity).collect{ case awsIdentity: AwsIdentity =>
    new ElkLogging(awsIdentity, config.loggingStreamName, creds, applicationLifecycle)
  }

  val languageToolFactory = new LanguageToolFactory(config.ngramPath, true)

  val guardianContentClient = GuardianContentClient(config.capiApiKey)
  val contentClient = new ContentClient(guardianContentClient)

  private val s3Client = AmazonS3ClientBuilder.standard().withCredentials(creds).withRegion(AppIdentity.region).build()

  val settingsFile = identity match {
    case identity: AwsIdentity if identity.stage == "PROD" => "gutools.co.uk.settings.public"
    case identity: AwsIdentity => s"${identity.stage.toLowerCase}.dev-gutools.co.uk.settings.public"
    case _: DevIdentity => "local.dev-gutools.co.uk.settings.public"
  }
  val publicSettings = new PublicSettings(settingsFile, "pan-domain-auth-settings", s3Client)
  publicSettings.start()

  val stage = identity match {
    case identity: AwsIdentity => identity.stage.toLowerCase
    case _ => "code"
  }
  val typerighterBucket = s"typerighter-app-${stage}"

  val cloudWatchClient = identity match {
    case identity: AwsIdentity => new CloudWatchClient(stage, false)
    case _ : DevIdentity => new CloudWatchClient(stage, true)
  }

  val matcherPoolDispatcher = actorSystem.dispatchers.lookup("matcher-pool-dispatcher")
  val defaultFutures = new DefaultFutures(actorSystem)
  val matcherPool = new MatcherPool(futures = defaultFutures, maybeCloudWatchClient = Some(cloudWatchClient))(matcherPoolDispatcher, materializer)

  val bucketRuleManager = new BucketRuleManager(s3Client, typerighterBucket, stage)
  val ruleProvisioner = new RuleProvisionerService(bucketRuleManager, matcherPool, languageToolFactory, cloudWatchClient)

  val sheetsRuleManager = new SheetsRuleManager(config.credentials, config.spreadsheetId, matcherPool, languageToolFactory)

  val apiController = new ApiController(controllerComponents, matcherPool, publicSettings)(executionContext)
  val rulesController = new RulesController(controllerComponents, matcherPool, sheetsRuleManager, bucketRuleManager, config.spreadsheetId, ruleProvisioner, publicSettings)
  val homeController = new HomeController(controllerComponents, publicSettings)
  val auditController = new AuditController(controllerComponents, publicSettings)
  val capiProxyController = new CapiProxyController(controllerComponents, contentClient, publicSettings)

  override lazy val httpErrorHandler = PreferredMediaTypeHttpErrorHandler(
    "application/json" -> new JsonHttpErrorHandler(environment, None),
    "text/html" -> new DefaultHttpErrorHandler(),
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

  /**
    * Set up matchers and add them to the matcher pool as the app starts.
    */
  ruleProvisioner.scheduleUpdateRules(actorSystem.scheduler)
}
