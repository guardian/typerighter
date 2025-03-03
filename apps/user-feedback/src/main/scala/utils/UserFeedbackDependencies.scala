package utils

import services.{LambdaAuth, SNSEventSender}

/** A trait containing dependencies that may be overridden for testing purposes.
  */
trait UserFeedbackDependencies {
  lazy val config: UserFeedbackConfig = new UserFeedbackConfig
  lazy val auth: LambdaAuth = new LambdaAuth
  lazy val snsEventSender: SNSEventSender = new SNSEventSender(config.snsClient)
}
