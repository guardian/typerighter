package services

import com.amazonaws.services.lambda.runtime.LambdaLogger
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest

class SNSEventSender(val snsClient: SnsClient, val logger: LambdaLogger) {
  def sendEvent(message: String, topicArn: String): Unit = {
    val publishRequest = PublishRequest.builder().message(message).topicArn(topicArn).build()
    val publishResponse = snsClient.publish(publishRequest)
    logger.log(s"Successfully published message with id ${publishResponse.messageId}, content: $message")
  }
}
