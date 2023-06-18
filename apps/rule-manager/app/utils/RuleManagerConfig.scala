package utils

import play.api.Configuration
import com.gu.typerighter.lib.CommonConfig
import com.gu.AppIdentity
import com.amazonaws.auth.AWSCredentialsProvider
import play.api.libs.ws.WSClient

class RuleManagerConfig(
    playConfig: Configuration,
    region: String,
    identity: AppIdentity,
    creds: AWSCredentialsProvider,
    ws: WSClient
) extends CommonConfig(playConfig, region, identity, creds, ws) {
  val dbUrl = playConfig.get[String]("db.default.url")
  val dbUsername = playConfig.get[String]("db.default.username")
  val dbPassword = playConfig.get[String]("db.default.password")
  val credentials = playConfig.get[String]("typerighter.google.credentials")
  val spreadsheetId = playConfig.get[String]("typerighter.sheetId")
}
