package db

import play.api.libs.json.{Format, JsNumber, JsResult, JsValue, OFormat}

case class WithId[T](entity: T, id: Int)

object WithId {
  implicit def formatter[T](implicit jf: OFormat[T]): Format[WithId[T]] = new Format[WithId[T]] {
    override def reads(json: JsValue): JsResult[WithId[T]] = {
      for {
        id <- (json \ "id").validate[Int]
        entity <- jf.reads(json)
      } yield WithId(entity, id)
    }

    override def writes(o: WithId[T]): JsValue = {
      jf.writes(o.entity) + ("id" -> JsNumber(o.id))
    }
  }
}