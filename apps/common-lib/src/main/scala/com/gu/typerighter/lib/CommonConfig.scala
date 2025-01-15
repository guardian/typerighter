package com.gu.typerighter.lib

import play.api.Configuration
import com.gu.AppIdentity
import com.gu.AwsIdentity
import com.gu.DevIdentity
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest
import com.gu.pandomainauth.PanDomainAuthSettingsRefresher
import com.gu.pandomainauth.S3BucketLoader
import play.api.libs.ws.WSClient

import scala.util.Try

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
) extends Loggable {
  val serviceName: String
  val capiApiKey = playConfig.get[String]("capi.apiKey")

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

  val panDomainSettings = PanDomainAuthSettingsRefresher(
    domain = stageDomain,
    system = appName,
    s3BucketLoader = S3BucketLoader.forAwsSdkV1(pandaS3Client, "pan-domain-auth-settings")
  )

  private val secretsManagerClient = AWSSecretsManagerClientBuilder
    .standard()
    .withCredentials(awsCredentials)
    .withRegion(awsRegion)
    .build()

  private val hmacSecretStages = List("AWSCURRENT", "AWSPREVIOUS")

  val hmacSecrets: List[String] = hmacSecretStages.flatMap { secretStage =>
    val secretId = s"/${stage.toUpperCase}/flexible/typerighter/hmacSecretKey"
    val getSecretValueRequest = new GetSecretValueRequest()
      .withSecretId(secretId)
      .withVersionStage(secretStage)

    val result = Try {
      val result = secretsManagerClient
        .getSecretValue(getSecretValueRequest)
        .getSecretString
      Some(result)
    }.recover { error =>
      log.warn(s"Could not fetch secret for $secretId, stage $secretStage", error)
      None
    }.get

    result
  }
}
