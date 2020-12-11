package com.gu.typerighter.lib

import play.api.Configuration
import com.gu.AppIdentity
import com.gu.AwsIdentity
import com.gu.DevIdentity

/**
  * A class to store configuration that's common across projects.
  *
  * Fails fast with an exception if properties aren't found.
  */
abstract class CommonConfig(playConfig: Configuration, identity: AppIdentity) {
  val dbUrl = playConfig.get[String]("db.default.url")
  val dbUsername = playConfig.get[String]("db.default.username")
  val dbPassword = playConfig.get[String]("db.default.password")

  val loggingStreamName = playConfig.getOptional[String]("typerighter.loggingStreamName")

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
