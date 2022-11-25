import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProvider, AWSCredentialsProviderChain, DefaultAWSCredentialsProviderChain}
import software.amazon.awssdk.auth.credentials.{AwsCredentialsProvider => AwsCredentialsProviderV2, DefaultCredentialsProvider => DefaultCredentialsProviderV2, ProfileCredentialsProvider => ProfileCredentialsProviderV2}
import com.gu.conf.{ConfigurationLoader, SSMConfigurationLocation}
import com.gu.{AppIdentity, AwsIdentity, DevIdentity}
import play.api.ApplicationLoader.Context
import play.api._
import com.gu.AppIdentity
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.gu.DevIdentity
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.gu.AwsIdentity
import com.gu.conf.SSMConfigurationLocation
import com.gu.conf.ConfigurationLoader
import com.gu.typerighter.lib.AppSetup
import play.api.Mode.Dev


class AppLoader extends ApplicationLoader {
  def load(context: Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }

    val appSetup = AppSetup(context)

    new AppComponents(
      context.copy(initialConfiguration = Configuration(appSetup.config).withFallback(context.initialConfiguration)),
      appSetup.region,
      appSetup.identity,
      appSetup.creds
    ).application
  }
}
