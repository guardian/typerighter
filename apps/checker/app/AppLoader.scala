import com.gu.typerighter.lib.AppSetup
import play.api.ApplicationLoader.Context
import play.api._

class AppLoader extends ApplicationLoader {
  def load(context: Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }

    val appSetup = AppSetup(context)

    new AppComponents(
      context.copy(initialConfiguration =
        Configuration(appSetup.config).withFallback(context.initialConfiguration)
      ),
      appSetup.region,
      appSetup.identity,
      appSetup.creds,
      appSetup.credsV2
    ).application
  }
}
