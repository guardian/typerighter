package com.gu.typerighter.model

import play.api.libs.json.{Json, Reads, Writes}

sealed trait TaggedWordlist {
  val tag: String
  val words: List[String]
}

case class WordTag(
    word: String,
    tag: String
)
case class WordlistsToNotPublish(rules: List[TaggedWordlist])

object WordlistsToNotPublish {
  implicit val writes: Writes[WordlistsToNotPublish] = Json.writes[WordlistsToNotPublish]
  implicit val reads: Reads[WordlistsToNotPublish] = Json.reads[WordlistsToNotPublish]
}
