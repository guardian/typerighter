package utils

import java.io.File
import com.gu.AppIdentity
import com.gu.typerighter.lib.CommonConfig
import play.api.Configuration
import com.amazonaws.auth.AWSCredentialsProvider
import play.api.libs.ws.WSClient

class CheckerConfig(
    playConfig: Configuration,
    region: String,
    identity: AppIdentity,
    creds: AWSCredentialsProvider,
    ws: WSClient
) extends CommonConfig(playConfig, region, identity, creds, ws) {
  val serviceName = "checker"
  val ngramPath: Option[File] =
    playConfig.getOptional[String]("typerighter.ngramPath").map(new File(_))
  val credentials = playConfig.get[String]("typerighter.google.credentials")
  val spreadsheetId = playConfig.get[String]("typerighter.sheetId")
}
