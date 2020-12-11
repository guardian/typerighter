package utils

import play.api.Configuration
import com.gu.typerighter.lib.CommonConfig
import com.gu.AppIdentity
import com.amazonaws.auth.AWSCredentialsProvider

class RuleManagerConfig(playConfig: Configuration, identity: AppIdentity, creds: AWSCredentialsProvider) extends CommonConfig(playConfig, identity, creds) {
  val dbUrl = playConfig.get[String]("db.default.url")
  val dbUsername = playConfig.get[String]("db.default.username")
  val dbPassword = playConfig.get[String]("db.default.password")
}
