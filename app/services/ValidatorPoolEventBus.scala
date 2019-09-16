package services

import akka.event.{EventBus, LookupClassification}
import model.ValidatorResponse

case class ValidatorPoolSubscriber(validationSetId: String, onEvent: ValidatorPoolEvent => Unit)

sealed trait ValidatorPoolEvent {
  val validationSetId: String
  val payload: Any
}

case class ValidatorPoolResultEvent(override val validationSetId: String, payload: ValidatorResponse) extends ValidatorPoolEvent
case class ValidatorPoolJobsCompleteEvent(override val validationSetId: String, payload: Unit = ()) extends ValidatorPoolEvent

class ValidatorPoolEventBus extends EventBus with LookupClassification {
  override type Event = ValidatorPoolEvent
  override type Classifier = String
  override type Subscriber = ValidatorPoolSubscriber

  override protected def mapSize(): Int = 128

  override protected def compareSubscribers(a: ValidatorPoolSubscriber, b: ValidatorPoolSubscriber): Int =
    if (a.validationSetId > b.validationSetId) 1 else if (a.validationSetId == b.validationSetId) 0 else -1

  override protected def classify(event: ValidatorPoolEvent): String = event.validationSetId

  override protected def publish(event: ValidatorPoolEvent, subscriber: ValidatorPoolSubscriber): Unit = subscriber.onEvent(event)
}
