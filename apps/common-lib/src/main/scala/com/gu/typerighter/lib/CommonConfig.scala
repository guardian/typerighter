package com.gu.typerighter.lib

import play.api.Configuration
import com.gu.AppIdentity
import com.gu.AwsIdentity
import com.gu.DevIdentity
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
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
    val awsCredentials: AwsCredentialsProvider,
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

  private val pandaS3Client = S3Client
    .builder()
    .credentialsProvider(awsCredentials)
    .region(Region.of(awsRegion))
    .build()

  val panDomainSettings = PanDomainAuthSettingsRefresher(
    domain = stageDomain,
    system = appName,
    s3BucketLoader = S3BucketLoader.forAwsSdkV2(pandaS3Client, "pan-domain-auth-settings")
  )

  private val secretsManagerClient = SecretsManagerClient
    .builder()
    .credentialsProvider(awsCredentials)
    .region(Region.of(awsRegion))
    .build()

  private val hmacSecretStages = List("AWSCURRENT", "AWSPREVIOUS")

  val hmacSecrets: List[String] = hmacSecretStages.flatMap { secretStage =>
    val secretId = s"/${stage.toUpperCase}/flexible/typerighter/hmacSecretKey"
    val getSecretValueRequest = GetSecretValueRequest
      .builder()
      .secretId(secretId)
      .versionStage(secretStage)
      .build()

    val result = Try {
      val result = secretsManagerClient
        .getSecretValue(getSecretValueRequest)
        .secretString
      Some(result)
    }.recover { error =>
      log.warn(s"Could not fetch secret for $secretId, stage $secretStage", error)
      None
    }.get

    result
  }
}
