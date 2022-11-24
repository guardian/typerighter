package utils

import java.io.File

import com.gu.AppIdentity
import com.gu.typerighter.lib.CommonConfig
import play.api.Configuration
import com.amazonaws.auth.AWSCredentialsProvider

class CheckerConfig(playConfig: Configuration, region: String, identity: AppIdentity, creds: AWSCredentialsProvider) extends CommonConfig(playConfig, region, identity, creds) {
  val ngramPath: Option[File] = playConfig.getOptional[String]("typerighter.ngramPath").map(new File(_))
  val capiApiKey = playConfig.get[String]("capi.apiKey")
  val credentials = playConfig.get[String]("typerighter.google.credentials")
  val spreadsheetId = playConfig.get[String]("typerighter.sheetId")
}
