package services

import akka.event.{EventBus, LookupClassification}
import model.MatcherResponse

case class MatcherPoolSubscriber(requestId: String, onEvent: MatcherPoolEvent => Unit)

sealed trait MatcherPoolEvent {
  val requestId: String
  val payload: Any
}

case class MatcherPoolResultEvent(override val requestId: String, payload: MatcherResponse) extends MatcherPoolEvent
case class MatcherPoolJobsCompleteEvent(override val requestId: String, payload: Unit = ()) extends MatcherPoolEvent

class MatcherPoolEventBus extends EventBus with LookupClassification {
  override type Event = MatcherPoolEvent
  override type Classifier = String
  override type Subscriber = MatcherPoolSubscriber

  override protected def mapSize(): Int = 128

  override protected def compareSubscribers(a: MatcherPoolSubscriber, b: MatcherPoolSubscriber): Int =
    if (a.requestId > b.requestId) 1 else if (a.requestId == b.requestId) 0 else -1

  override protected def classify(event: MatcherPoolEvent): String = event.requestId

  override protected def publish(event: MatcherPoolEvent, subscriber: MatcherPoolSubscriber): Unit = subscriber.onEvent(event)
}
