import java.io.File

import scala.concurrent.Future
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, InstanceProfileCredentialsProvider}
import com.gu.contentapi.client.GuardianContentClient
import com.gu.{AppIdentity, AwsIdentity}
import controllers.{ApiController, CapiProxyController, HomeController, RulesController, AuditController}
import matchers.{LanguageToolFactory, RegexMatcher}
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.http.{DefaultHttpErrorHandler, JsonHttpErrorHandler, PreferredMediaTypeHttpErrorHandler}
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.EssentialFilter
import play.filters.HttpFiltersComponents
import play.filters.cors.CORSComponents
import router.Routes
import rules.SheetsRuleResource
import services.{ElkLogging, MatcherPool}
import services._
import utils.Loggable

class AppComponents(context: Context, identity: AppIdentity)
  extends BuiltInComponentsFromContext(context)
  with HttpFiltersComponents
  with CORSComponents
  with Loggable
  with controllers.AssetsComponents
  with AhcWSComponents {

  override def httpFilters: Seq[EssentialFilter] = corsFilter +: super.httpFilters.filterNot(allowedHostsFilter ==)

  private val awsCredentialsProvider = new AWSCredentialsProviderChain(
    InstanceProfileCredentialsProvider.getInstance(),
    new ProfileCredentialsProvider(configuration.get[String]("typerighter.defaultAwsProfile"))
  )

  // initialise log shipping if we are in AWS
  private val logShipping = Some(identity).collect{ case awsIdentity: AwsIdentity =>
    val loggingStreamName = configuration.getOptional[String]("typerighter.loggingStreamName")
    new ElkLogging(awsIdentity, loggingStreamName, awsCredentialsProvider, applicationLifecycle)
  }

  val ngramPath: Option[File] = configuration.getOptional[String]("typerighter.ngramPath").map(new File(_))
  val matcherPoolDispatcher = actorSystem.dispatchers.lookup("matcher-pool-dispatcher")
  val matcherPool = new MatcherPool()(matcherPoolDispatcher, materializer)

  val credentials = configuration.get[String]("typerighter.google.credentials")
  val spreadsheetId = configuration.get[String]("typerighter.sheetId")
  val range = configuration.get[String]("typerighter.sheetRange")
  val ruleResource = new SheetsRuleResource(credentials, spreadsheetId, range)

  val capiApiKey = configuration.get[String]("capi.apiKey")
  val guardianContentClient = GuardianContentClient(capiApiKey)
  val contentClient = new ContentClient(guardianContentClient)

  val apiController = new ApiController(controllerComponents, matcherPool)
  val rulesController = new RulesController(controllerComponents, matcherPool, ruleResource, spreadsheetId)
  val homeController = new HomeController(controllerComponents)
  val auditController = new AuditController(controllerComponents)
  val capiProxyController = new CapiProxyController(controllerComponents, contentClient)

  override lazy val httpErrorHandler = PreferredMediaTypeHttpErrorHandler(
    "application/json" -> new JsonHttpErrorHandler(environment, None),
    "text/html" -> new DefaultHttpErrorHandler(),
  )

  initialiseMatchers

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
  def initialiseMatchers: Future[Unit] = {
    for {
      (rulesByCategory, _) <- ruleResource.fetchRulesByCategory()
    } yield {
      rulesByCategory.foreach { case (category, rules) => {
        val matcher = new RegexMatcher(category.name, rules)
        matcherPool.addMatcher(category, matcher)
      }}
    }
  }
}
