package actors

import model.{Check, ValidatorError}
import play.api.libs.json.{JsValue, Json}
import services._

import scala.concurrent.{ExecutionContext}
import akka.actor._

object WsCheckQueryActor {
  def props(out: ActorRef, pool: ValidatorPool)(implicit ec: ExecutionContext) = Props(new WsCheckQueryActor(out, pool))
}

class WsCheckQueryActor(out: ActorRef, pool: ValidatorPool)(implicit ec: ExecutionContext) extends Actor {
  def receive = {
    case jsValue: JsValue =>
      jsValue match {
        case jsCheck if jsValue.validateOpt[Check].isSuccess => {
          val check = jsCheck.validate[Check].get
          val onEvent = (e: ValidatorPoolEvent) => {
            e match {
              case ValidatorPoolResultEvent(_, response) => out ! Json.toJson(response)
              case _: ValidatorPoolJobsCompleteEvent => out ! PoisonPill
            }
            ()
          }
          val subscriber = ValidatorPoolSubscriber(check.validationSetId, onEvent)
          pool.check(check)
          pool.subscribe(subscriber)
        }
        case _ =>
          out ! Json.toJson(ValidatorError("Error parsing input"))
          out ! PoisonPill
      }
  }
}
