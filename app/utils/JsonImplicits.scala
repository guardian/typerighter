package utils

import java.net.URL

import play.api.libs.json.{JsString, Writes}

object JsonImplicits {
  implicit val urlWrites: Writes[URL] = (o: URL) => JsString(o.toString)
}
