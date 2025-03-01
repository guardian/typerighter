package services

import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest

class SNSEventSender {
  def sendEvent(snsClient: SnsClient, topicArn: String, message: String): Unit = {
    val publishRequest = PublishRequest.builder().message(message).topicArn(topicArn).build()
    snsClient.publish(publishRequest)
  }
}
