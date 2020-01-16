import java.io.File

import scala.concurrent.Future
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, InstanceProfileCredentialsProvider}
import com.gu.{AppIdentity, AwsIdentity}
import controllers.{ApiController, HomeController, RulesController}
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.http.{DefaultHttpErrorHandler, JsonHttpErrorHandler, PreferredMediaTypeHttpErrorHandler}
import play.api.mvc.EssentialFilter
import play.filters.HttpFiltersComponents
import play.filters.cors.CORSComponents
import router.Routes
import rules.SheetsRuleResource
import services.{ElkLogging, LanguageToolFactory, MatcherPool}
import services._
import utils.Loggable

class AppComponents(context: Context, identity: AppIdentity)
  extends BuiltInComponentsFromContext(context)
  with HttpFiltersComponents
  with CORSComponents
  with Loggable
  with controllers.AssetsComponents {

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
  val languageToolFactory = new LanguageToolFactory(ngramPath)
  val matcherPoolDispatcher = actorSystem.dispatchers.lookup("matcher-pool-dispatcher")
  val matcherPool = new MatcherPool()(matcherPoolDispatcher, materializer)

  val credentials = configuration.get[String]("typerighter.google.credentials")
  val spreadsheetId = configuration.get[String]("typerighter.sheetId")
  val range = configuration.get[String]("typerighter.sheetRange")
  val ruleResource = new SheetsRuleResource(credentials, spreadsheetId, range)


  val apiController = new ApiController(controllerComponents, matcherPool)
  val rulesController = new RulesController(controllerComponents, matcherPool, languageToolFactory, ruleResource, spreadsheetId)
  val homeController = new HomeController(controllerComponents)


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
    apiController
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
