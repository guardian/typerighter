package model

import play.api.libs.json.{Json, Reads, Writes}

object Suggestion {
  implicit val writes: Writes[Suggestion] = {
    case textSuggestion: TextSuggestion =>
      TextSuggestion.writes.writes(textSuggestion)
  }
}

sealed trait Suggestion {
  val `type`: String
  val text: String
}

object TextSuggestion {
  implicit val reads: Reads[TextSuggestion] = Json.reads[TextSuggestion]
  implicit val writes = new Writes[TextSuggestion] {
    def writes(suggestion: TextSuggestion) = Json.obj(
      "type" -> suggestion.`type`,
      "text" -> suggestion.text
    )
  }
}

case class TextSuggestion(text: String) extends Suggestion {
  val `type` = "TEXT_SUGGESTION"
}


