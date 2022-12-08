package com.gu.typerighter.lib

import play.api.Configuration
import com.gu.AppIdentity
import com.gu.AwsIdentity
import com.gu.DevIdentity
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.regions.Region

/**
  * A class to store configuration that's common across projects.
  *
  * Fails fast with an exception if properties aren't found.
  */
abstract class CommonConfig(playConfig: Configuration, region: String, identity: AppIdentity, credentials: AWSCredentialsProvider) {
  val awsCredentials = credentials
  val awsRegion = region
  val loggingStreamName = playConfig.getOptional[String]("typerighter.loggingStreamName")

  val permissionsBucket = playConfig.getOptional[String]("permissions.bucket").getOrElse("permissions-cache")

  val stage = identity match {
    case identity: AwsIdentity => identity.stage.toLowerCase
    case _ => "code"
  }

  val stageDomain = identity match {
    case identity: AwsIdentity if identity.stage == "PROD" => "gutools.co.uk"
    case identity: AwsIdentity => s"${identity.stage.toLowerCase}.dev-gutools.co.uk"
    case _: DevIdentity => "local.dev-gutools.co.uk"
  }

  val appName = identity match {
    case identity: AwsIdentity => identity.app
    case identity: DevIdentity => identity.app
  }
}
