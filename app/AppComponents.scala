import java.io.File

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, InstanceProfileCredentialsProvider}
import com.gu.{AppIdentity, AwsIdentity}
import controllers.{ApiController, HomeController, RulesController}
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.mvc.EssentialFilter
import play.filters.HttpFiltersComponents
import play.filters.cors.CORSComponents
import router.Routes
import rules.SheetsRuleResource
import services.{ElkLogging, LanguageToolFactory, ValidatorPool}
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
  val validatorPool = new ValidatorPool


  val credentials = configuration.get[String]("typerighter.google.credentials")
  val spreadsheetId = configuration.get[String]("typerighter.sheetId")
  val range = configuration.get[String]("typerighter.sheetRange")
  val ruleResource = new SheetsRuleResource(credentials, spreadsheetId, range)

  val apiController = new ApiController(controllerComponents, validatorPool, ruleResource)
  val rulesController = new RulesController(controllerComponents, validatorPool, languageToolFactory, ruleResource, spreadsheetId)
  val homeController = new HomeController(controllerComponents)

  // Fetch the rules when the app starts.
  for {
    (rules, _) <- ruleResource.fetchRulesByCategory()
  } yield {
    rules.foreach { case (category, rules) => {
      val (validator, _) = languageToolFactory.createInstance(category.name, ValidatorConfig(rules))
      validatorPool.addValidator(category, validator)
    }}
  }

  lazy val router = new Routes(
    httpErrorHandler,
    assets,
    homeController,
    rulesController,
    apiController
  )
}