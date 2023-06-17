package utils

import java.net.URL
import play.api.libs.json.{JsString, Json, Writes}

object JsonHelpers {
  implicit val urlWrites: Writes[URL] = (o: URL) => JsString(o.toString)
  def toNDJson[T](serializable: T)(implicit tjs: Writes[T]) =
    Json.toJson(serializable).toString() + 31.toChar
}
