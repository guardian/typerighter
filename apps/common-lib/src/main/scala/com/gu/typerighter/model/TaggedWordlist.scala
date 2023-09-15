package com.gu.typerighter.model

import play.api.libs.json.{Json, Reads, Writes}

case class TaggedWordlist(
    tag: String,
    words: List[String]
)

object TaggedWordlist {
  implicit val writes: Writes[TaggedWordlist] = Json.writes[TaggedWordlist]
  implicit val reads: Reads[TaggedWordlist] = Json.reads[TaggedWordlist]
}

case class WordTag(
    word: String,
    tag: String
)
