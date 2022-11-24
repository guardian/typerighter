import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProvider, DefaultAWSCredentialsProviderChain}
import software.amazon.awssdk.auth.credentials.{AwsCredentialsProvider => AwsCredentialsProviderV2, DefaultCredentialsProvider => DefaultCredentialsProviderV2, ProfileCredentialsProvider => ProfileCredentialsProviderV2}
import com.gu.conf.{ConfigurationLoader, SSMConfigurationLocation}
import com.gu.{AppIdentity, AwsIdentity, DevIdentity}
import play.api.ApplicationLoader.Context
import play.api.Mode.Dev
import play.api._


class AppLoader extends ApplicationLoader {
  def load(context: Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }

    val region = context.initialConfiguration.getOptional[String]("aws.region").getOrElse("eu-west-1")

    val (creds, credsV2, identity) = context.environment.mode match {
      case Dev => (
        new ProfileCredentialsProvider("composer"),
        ProfileCredentialsProviderV2.create("composer"),
        DevIdentity("typerighter-checker")
      )
      case _ =>
      val credsV2 = DefaultCredentialsProviderV2.create();
      (
        DefaultAWSCredentialsProviderChain.getInstance,
        credsV2,
        AppIdentity.whoAmI(defaultAppName = "typerighter-checker", credsV2).get
      )
    }

    val loadedConfig = ConfigurationLoader.load(identity, credsV2) {
      case identity: AwsIdentity => SSMConfigurationLocation.default(identity)
      case development: DevIdentity => SSMConfigurationLocation(s"/DEV/flexible/${development.app}", region)
    }

    new AppComponents(
      context.copy(initialConfiguration = Configuration(loadedConfig).withFallback(context.initialConfiguration)),
      region,
      identity,
      creds,
      credsV2
    ).application
  }
}
