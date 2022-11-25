package com.gu.typerighter.lib

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProvider, DefaultAWSCredentialsProviderChain}
import software.amazon.awssdk.auth.credentials.{AwsCredentialsProvider => AwsCredentialsProviderV2, DefaultCredentialsProvider => DefaultCredentialsProviderV2, ProfileCredentialsProvider => ProfileCredentialsProviderV2}
import com.gu.conf.{ConfigurationLoader, SSMConfigurationLocation}
import com.gu.{AppIdentity, AwsIdentity, DevIdentity}
import com.typesafe.config.Config
import play.api.Mode.Dev
import play.api.ApplicationLoader

case class AppSetup(
                     region: String,
                     creds: AWSCredentialsProvider,
                     credsV2: AwsCredentialsProviderV2,
                     identity: AppIdentity,
                     config: Config
                   )

object AppSetup {
  def apply(context: ApplicationLoader.Context): AppSetup = {
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

    val config = ConfigurationLoader.load(identity, credsV2) {
      case identity: AwsIdentity => SSMConfigurationLocation.default(identity)
      case development: DevIdentity => SSMConfigurationLocation(s"/DEV/flexible/${development.app}", region)
    }

    AppSetup(region, creds, credsV2, identity, config)
  }
}
