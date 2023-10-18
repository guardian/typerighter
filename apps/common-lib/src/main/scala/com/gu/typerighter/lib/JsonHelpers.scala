package com.gu.typerighter.lib

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Framing}
import akka.util.ByteString
import play.api.libs.json.{JsString, Json, Writes}

import java.net.URL

object JsonHelpers {
  val recordSeparatorChar = 31.toChar
  implicit val urlWrites: Writes[URL] = (o: URL) => JsString(o.toString)

  def toJsonSeq[T](serializable: T)(implicit tjs: Writes[T]) =
    Json.toJson(serializable).toString() + recordSeparatorChar

  def toNewlineDelineatedJson[T](serializable: T)(implicit tjs: Writes[T]) =
    Json.toJson(serializable).toString() + "\n"

  val JsonSeqFraming: Flow[ByteString, ByteString, NotUsed] =
    Framing.delimiter(ByteString(recordSeparatorChar), Int.MaxValue, true)
}
