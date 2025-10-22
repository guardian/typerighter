import play.api.ApplicationLoader.Context
import play.filters.HttpFiltersComponents
import play.api.BuiltInComponentsFromContext
import play.api.libs.ws.ahc.AhcWSComponents
import controllers.{AssetsComponents, HomeController, RulesController, TagsController}
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import com.gu.AwsIdentity
import com.gu.AppIdentity
import com.gu.DevIdentity
import com.gu.contentapi.client.GuardianContentClient
import com.gu.typerighter.lib.{ContentClient, HMACClient}
import com.gu.typerighter.rules.BucketRuleResource
import router.Routes
import db.DB
import play.api.db.evolutions.EvolutionsComponents
import play.api.db.{DBComponents, HikariCPComponents}
import service.{DictionaryResource, RuleTesting, SheetsRuleResource}
import utils.{LocalStack, RuleManagerConfig}

class AppComponents(
    context: Context,
    region: String,
    identity: AppIdentity,
    creds: AwsCredentialsProvider
) extends BuiltInComponentsFromContext(context)
    with HttpFiltersComponents
    with AssetsComponents
    with DBComponents
    with HikariCPComponents
    with EvolutionsComponents
    with AhcWSComponents {
  val config = new RuleManagerConfig(configuration, region, identity, creds, wsClient)
  val db = new DB(config.dbUrl, config.dbUsername, config.dbPassword)

  applicationEvolutions

  private val standardS3Client = S3Client
    .builder()
    .credentialsProvider(creds)
    .region(Region.of(region))
    .build()

  private val s3Client = identity match {
    case _: AwsIdentity => standardS3Client
    case _: DevIdentity =>
      LocalStack.s3Client
  }

  val stage = identity match {
    case identity: AwsIdentity => identity.stage.toLowerCase
    case _: DevIdentity        => "local"
  }
  val typerighterBucket = s"typerighter-app-${stage}"
  val hmacClient = new HMACClient(stage, secretKey = config.hmacSecrets.head)
  val sheetsRuleResource = new SheetsRuleResource(config.credentials, config.spreadsheetId)
  val bucketRuleResource = new BucketRuleResource(s3Client, typerighterBucket, stage)
  val dictionaryResource = new DictionaryResource(s3Client, typerighterBucket, stage)
  val guardianContentClient = GuardianContentClient(config.capiApiKey)
  val contentClient = new ContentClient(guardianContentClient)
  val ruleTesting = new RuleTesting(wsClient, hmacClient, contentClient, config.checkerServiceUrl)

  val homeController = new HomeController(
    controllerComponents,
    db,
    config
  )

  val rulesController = new RulesController(
    controllerComponents,
    sheetsRuleResource,
    bucketRuleResource,
    dictionaryResource,
    ruleTesting,
    config
  )

  val tagsController = new TagsController(
    controllerComponents,
    config
  )

  lazy val router = new Routes(
    httpErrorHandler,
    homeController,
    rulesController,
    tagsController,
    assets
  )
}
