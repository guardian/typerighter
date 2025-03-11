package utils

import play.api.Configuration
import com.gu.typerighter.lib.CommonConfig
import com.gu.AppIdentity
import com.amazonaws.auth.AWSCredentialsProvider
import play.api.libs.ws.WSClient
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient

class RuleManagerConfig(
    playConfig: Configuration,
    region: String,
    identity: AppIdentity,
    creds: AWSCredentialsProvider,
    credsV2: AwsCredentialsProvider,
    ws: WSClient
) extends CommonConfig(playConfig, region, identity, creds, ws) {
  val serviceName = "manager"
  val dbUrl = playConfig.get[String]("db.default.url")
  val dbUsername = playConfig.get[String]("db.default.username")
  val dbPassword = playConfig.get[String]("db.default.password")
  val credentials = playConfig.get[String]("typerighter.google.credentials")
  val spreadsheetId = playConfig.get[String]("typerighter.sheetId")
  val checkerServiceUrl = playConfig.get[String]("typerighter.checkerServiceUrl")

  val snsClient: SqsClient = SqsClient
    .builder()
    .region(Region.EU_WEST_1)
    .credentialsProvider(credsV2)
    .build()
}
