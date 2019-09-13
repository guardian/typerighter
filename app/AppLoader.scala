import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, DefaultAWSCredentialsProviderChain}
import com.gu.conf.{ConfigurationLoader, SSMConfigurationLocation}
import com.gu.{AppIdentity, AwsIdentity, DevIdentity}
import play.api.ApplicationLoader.Context
import play.api._

class AppLoader extends ApplicationLoader {
  def load(context: Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }
    
    val identity = AppIdentity.whoAmI(defaultAppName = "typerighter")

    val creds = identity match {
      case _: DevIdentity => new ProfileCredentialsProvider("composer")
      case _ => DefaultAWSCredentialsProviderChain.getInstance
    }

    val loadedConfig = ConfigurationLoader.load(identity, creds) {
      case identity: AwsIdentity => SSMConfigurationLocation.default(identity)
      case development: DevIdentity => SSMConfigurationLocation(s"/DEV/flexible/${development.app}")
    }

    new AppComponents(
      context.copy(initialConfiguration = context.initialConfiguration ++ Configuration(loadedConfig)),
      identity
    ).application
  }
}