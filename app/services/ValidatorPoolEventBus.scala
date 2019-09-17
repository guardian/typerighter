package services

import akka.event.{EventBus, LookupClassification}
import model.ValidatorResponse

case class ValidatorPoolSubscriber(requestId: String, onEvent: ValidatorPoolEvent => Unit)

sealed trait ValidatorPoolEvent {
  val requestId: String
  val payload: Any
}

case class ValidatorPoolResultEvent(override val requestId: String, payload: ValidatorResponse) extends ValidatorPoolEvent
case class ValidatorPoolJobsCompleteEvent(override val requestId: String, payload: Unit = ()) extends ValidatorPoolEvent

class ValidatorPoolEventBus extends EventBus with LookupClassification {
  override type Event = ValidatorPoolEvent
  override type Classifier = String
  override type Subscriber = ValidatorPoolSubscriber

  override protected def mapSize(): Int = 128

  override protected def compareSubscribers(a: ValidatorPoolSubscriber, b: ValidatorPoolSubscriber): Int =
    if (a.requestId > b.requestId) 1 else if (a.requestId == b.requestId) 0 else -1

  override protected def classify(event: ValidatorPoolEvent): String = event.requestId

  override protected def publish(event: ValidatorPoolEvent, subscriber: ValidatorPoolSubscriber): Unit = subscriber.onEvent(event)
}
