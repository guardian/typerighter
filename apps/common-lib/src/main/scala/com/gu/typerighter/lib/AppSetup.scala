package com.gu.typerighter.lib

import software.amazon.awssdk.auth.credentials.{
  AwsCredentialsProvider,
  DefaultCredentialsProvider,
  ProfileCredentialsProvider
}
import com.gu.conf.{ConfigurationLoader, SSMConfigurationLocation}
import com.gu.{AppIdentity, AwsIdentity, DevIdentity}
import com.typesafe.config.Config
import play.api.Mode.Dev
import play.api.ApplicationLoader

case class AppSetup(
    region: String,
    creds: AwsCredentialsProvider,
    identity: AppIdentity,
    config: Config
)

object AppSetup {
  def apply(context: ApplicationLoader.Context): AppSetup = {
    val region =
      context.initialConfiguration.getOptional[String]("aws.region").getOrElse("eu-west-1")

    val (credsV2, identity) = context.environment.mode match {
      case Dev =>
        (
          ProfileCredentialsProvider.create("composer"),
          DevIdentity("typerighter-checker")
        )
      case _ =>
        val credsV2 = DefaultCredentialsProvider.builder().build()
        (
          credsV2,
          AppIdentity.whoAmI(defaultAppName = "typerighter-checker", credsV2).get
        )
    }

    val config = ConfigurationLoader.load(identity, credsV2) {
      case identity: AwsIdentity => SSMConfigurationLocation.default(identity)
      case development: DevIdentity =>
        SSMConfigurationLocation(s"/DEV/flexible/${development.app}", region)
    }

    AppSetup(region, credsV2, identity, config)
  }
}
