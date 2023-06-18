package com.gu.typerighter.lib

import play.api.Configuration
import com.gu.AppIdentity
import com.gu.AwsIdentity
import com.gu.DevIdentity
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.s3.AmazonS3ClientBuilder
<<<<<<< HEAD
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest
=======
>>>>>>> 7130b55a (Refactor configuration to pass pan-domain-config as a part of CommonConfig, and adjust controller configuration to suit HMACAuthActions)
import com.gu.pandomainauth.PanDomainAuthSettingsRefresher
import play.api.libs.ws.WSClient

/** A class to store configuration that's common across projects.
  *
  * Fails fast with an exception if properties aren't found.
  */
abstract class CommonConfig(
    playConfig: Configuration,
    val awsRegion: String,
    identity: AppIdentity,
    val awsCredentials: AWSCredentialsProvider,
    val ws: WSClient
) {
<<<<<<< HEAD
=======

>>>>>>> 7130b55a (Refactor configuration to pass pan-domain-config as a part of CommonConfig, and adjust controller configuration to suit HMACAuthActions)
  val permissionsBucket =
    playConfig.getOptional[String]("permissions.bucket").getOrElse("permissions-cache")

  val stage = identity match {
    case identity: AwsIdentity => identity.stage.toLowerCase
    case _                     => "dev"
  }

  val stageDomain = identity match {
    case identity: AwsIdentity if identity.stage == "PROD" => "gutools.co.uk"
    case identity: AwsIdentity => s"${identity.stage.toLowerCase}.dev-gutools.co.uk"
    case _: DevIdentity        => "local.dev-gutools.co.uk"
  }

  val appName = identity match {
    case identity: AwsIdentity => identity.app
    case identity: DevIdentity => identity.app
  }

  private val pandaS3Client = AmazonS3ClientBuilder
    .standard()
    .withCredentials(awsCredentials)
    .withRegion(awsRegion)
    .build()

  val panDomainSettings = new PanDomainAuthSettingsRefresher(
    domain = stageDomain,
    system = appName,
    bucketName = "pan-domain-auth-settings",
    settingsFileKey = s"$stageDomain.settings",
    s3Client = pandaS3Client
  )
<<<<<<< HEAD

  private val secretsManagerClient = AWSSecretsManagerClientBuilder
    .standard()
    .withRegion(awsRegion)
    .build()

  private val hmacSecretStages = List("AWSCURRENT", "AWSPREVIOUS")
  lazy val hmacSecrets: List[String] = hmacSecretStages.map { stage =>
    val getSecretValueRequest = new GetSecretValueRequest()
      .withSecretId(s"/$stage/flexible/typerighter/hmacSecret")
      .withVersionStage(stage)

    secretsManagerClient
      .getSecretValue(getSecretValueRequest)
      .getSecretString
  }
=======
>>>>>>> 7130b55a (Refactor configuration to pass pan-domain-config as a part of CommonConfig, and adjust controller configuration to suit HMACAuthActions)
}
