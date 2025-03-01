package utils

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.gu.{AppIdentity, AwsIdentity, DevIdentity}
import com.gu.conf.{ConfigurationLoader, SSMConfigurationLocation}
import com.gu.pandomainauth.{PanDomainAuthSettingsRefresher, S3BucketLoader}
import software.amazon.awssdk.auth.credentials.{AwsCredentialsProviderChain => AwsCredentialsProviderChainV2, DefaultCredentialsProvider => DefaultCredentialsProviderV2}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sns.SnsClient

class UserFeedbackConfig {
  private val region = Region.EU_WEST_1
  val credsV1 = DefaultAWSCredentialsProviderChain.getInstance
  val credsV2 = AwsCredentialsProviderChainV2
    .builder()
    .credentialsProviders(
      DefaultCredentialsProviderV2.create()
    )
    .build()
  private val identity = AppIdentity.whoAmI(defaultAppName = "typerighter-user-feedback", credsV2).get
  private val config = ConfigurationLoader.load(identity, credsV2) {
    case identity: AwsIdentity => SSMConfigurationLocation.default(identity)
    case development: DevIdentity =>
      SSMConfigurationLocation(s"/DEV/flexible/${development.app}", region.toString)
  }

  val appName = identity match {
    case identity: AwsIdentity => identity.app
    case identity: DevIdentity => identity.app
  }

  val stageDomain = identity match {
    case identity: AwsIdentity if identity.stage == "PROD" => "gutools.co.uk"
    case identity: AwsIdentity => s"${identity.stage.toLowerCase}.dev-gutools.co.uk"
    case _: DevIdentity        => "local.dev-gutools.co.uk"
  }

  val userFeedbackSnsTopic = config.getString("userFeedback.snsTopicArn")

  val snsClient = SnsClient.builder()
    .region(Region.EU_WEST_1)
    .credentialsProvider(credsV2)
    .build();

  val s3Client = AmazonS3ClientBuilder
    .standard()
    .withRegion(region.toString)
    .withCredentials(credsV1)
    .build()

  val publicSettingsVerification = PanDomainAuthSettingsRefresher(
    domain = stageDomain,
    system = appName,
    s3BucketLoader = S3BucketLoader.forAwsSdkV1(s3Client, "pan-domain-auth-settings")
  ).settings.signingAndVerification
}
