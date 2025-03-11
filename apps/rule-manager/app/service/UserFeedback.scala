package service

import com.gu.typerighter.lib.Loggable
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.{Message, ReceiveMessageRequest}

import scala.jdk.CollectionConverters._

class UserFeedback(userFeedbackQueueArn: String, sqsClient: SqsClient) extends Loggable {
  def listenForMessages(): Unit = {
    for {
      message <- getNextMessage()
    } yield {
      log.info(s"Got message: $message")
    }
  }

  private def getNextMessage(): Option[Message] = {
    val receiveMessageRequest = ReceiveMessageRequest
      .builder()
      .queueUrl(userFeedbackQueueArn)
      .waitTimeSeconds(20)
      .maxNumberOfMessages(1)
      .build()

    sqsClient
      .receiveMessage(receiveMessageRequest)
      .messages()
      .asScala
      .toList
      .headOption
  }
}
