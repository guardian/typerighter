package actors

import model.{Check, ValidatorError, ValidatorWorkComplete}
import play.api.libs.json.{JsValue, Json}
import services._

import scala.concurrent.ExecutionContext
import akka.actor._

object WsCheckQueryActor {
  def props(out: ActorRef, pool: ValidatorPool)(implicit ec: ExecutionContext) = Props(new WsCheckQueryActor(out, pool))
}

class WsCheckQueryActor(out: ActorRef, pool: ValidatorPool)(implicit ec: ExecutionContext) extends Actor {
  def receive: PartialFunction[Any, Unit] = {
    case jsValue: JsValue =>
      jsValue match {
        case jsCheck if jsValue.validateOpt[Check].isSuccess => {
          val check = jsCheck.validate[Check].get
          val subscriber = ValidatorPoolSubscriber(check.requestId, onEvent)
          pool.check(check)
          pool.subscribe(subscriber)
        }
        case _ =>
          out ! Json.toJson(ValidatorError("Error parsing input"))
          out ! PoisonPill
      }
  }

  private def onEvent(event: ValidatorPoolEvent): Unit = event match {
    case ValidatorPoolResultEvent(_, response) => {
      println("sending", response.blocks.map(_.id).mkString(", "), response.categoryIds.mkString(", "))
      out ! Json.toJson(response)
    }
    case _: ValidatorPoolJobsCompleteEvent => {
      out ! Json.toJson(ValidatorWorkComplete())
      out ! PoisonPill
    }
  }
}
