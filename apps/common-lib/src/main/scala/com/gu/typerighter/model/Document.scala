package com.gu.typerighter.model

import play.api.libs.json.{Format, Json}

object Document {
  implicit val format: Format[Document] = Json.format[Document]
}

case class Document(id: String, blocks: List[TextBlock])
