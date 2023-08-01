package com.gu.typerighter.model

import com.gu.contentapi.client.model.v1.Content
import play.api.libs.json.{Format, Json}

object Document {
  implicit val format: Format[Document] = Json.format[Document]

  def fromCapiContent(content: Content): Document = {
    Document(
      content.id,
      content.blocks
        .flatMap(_.body)
        .getOrElse(Seq.empty)
        .flatMap(TextBlock.fromCAPIBlock)
        .toList
    )
  }
}

case class Document(id: String, blocks: List[TextBlock])
