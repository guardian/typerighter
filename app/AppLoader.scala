import com.gu.conf.{ConfigurationLoader, SSMConfigurationLocation}
import com.gu.{AppIdentity, AwsIdentity}
import play.api.ApplicationLoader.Context
import play.api._

class AppLoader extends ApplicationLoader {
  def load(context: Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }
    val identity = AppIdentity.whoAmI(defaultAppName = "typerighter")
    val loadedConfig = ConfigurationLoader.load(identity) {
      case identity: AwsIdentity => SSMConfigurationLocation.default(identity)
    }

    new AppComponents(
      context.copy(initialConfiguration = context.initialConfiguration ++ Configuration(loadedConfig)),
      identity
    ).application
  }
}