package actors

import model.{Check, ValidatorError, ValidatorResponse}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.streams.ActorFlow
import play.api.mvc._
import services._
import rules.RuleResource

import scala.concurrent.{ExecutionContext, Future}
import akka.actor._
import akka.stream.Materializer
import akka.actor.ActorSystem

object WsCheckQueryActor {
  def props(out: ActorRef, pool: ValidatorPool)(implicit ec: ExecutionContext) = Props(new WsCheckQueryActor(out, pool))
}

class WsCheckQueryActor(out: ActorRef, pool: ValidatorPool)(implicit ec: ExecutionContext) extends Actor {
  def receive = {
    case jsValue: JsValue =>
      jsValue match {
        case jsCheck if jsValue.validateOpt[Check].isSuccess => {
          var queriesComplete = 0
          val check = jsCheck.validate[Check].get
          check.inputs.foreach { query =>
            pool.check(query.validationId, query.text, query.categoryIds).map { results =>
              val response = ValidatorResponse(
                query.validationId,
                query.text,
                results
              )
              out ! Json.toJson(response)
            } recover {
              case e: Exception => out ! ValidatorError(query.validationId, e.getMessage)
            } andThen {
              case _ => {
                queriesComplete = queriesComplete + 1
                if (queriesComplete == check.inputs.size) {
                  out ! PoisonPill
                }
              }
            }
          }
        }
        case _ =>
          out ! ValidatorError("@@NONE@@", "Error parsing input")
          out ! PoisonPill
      }
      var queriesComplete = 0
  }
}
