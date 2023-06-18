import play.api.ApplicationLoader.Context
import play.filters.HttpFiltersComponents
import play.api.BuiltInComponentsFromContext
import play.api.libs.ws.ahc.AhcWSComponents
import controllers.{AssetsComponents, HomeController, RulesController}
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.auth.AWSCredentialsProvider
import com.gu.AwsIdentity
import com.gu.AppIdentity
import com.gu.DevIdentity
import com.gu.typerighter.rules.BucketRuleResource
import router.Routes
import db.DB
import play.api.db.evolutions.EvolutionsComponents
import play.api.db.{DBComponents, HikariCPComponents}
import service.SheetsRuleResource
import utils.{LocalStack, RuleManagerConfig}

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
  val config = new RuleManagerConfig(configuration, region, identity, creds, wsClient)
  val db = new DB(config.dbUrl, config.dbUsername, config.dbPassword)

  applicationEvolutions

  private val standardS3Client = AmazonS3ClientBuilder
    .standard()
    .withCredentials(creds)
    .withRegion(region)
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

  val sheetsRuleResource = new SheetsRuleResource(config.credentials, config.spreadsheetId)
  val bucketRuleResource = new BucketRuleResource(s3Client, typerighterBucket, stage)

  val homeController = new HomeController(
    controllerComponents,
    db,
    config
  )

  val rulesController = new RulesController(
    controllerComponents,
    sheetsRuleResource,
    bucketRuleResource,
    config
  )

  lazy val router = new Routes(
    httpErrorHandler,
    homeController,
    rulesController,
    assets
  )
}
