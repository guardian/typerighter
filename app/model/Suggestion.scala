package model

import play.api.libs.json.{Json, Reads, Writes}
import com.scalatsi._
import com.scalatsi.DefaultTSTypes._

sealed trait Suggestion {
  val `type`: String
  val text: String
}

object Suggestion {
  implicit val writes: Writes[Suggestion] = {
    case textSuggestion: TextSuggestion =>
      TextSuggestion.writes.writes(textSuggestion)
  }

  implicit val toTS = TSType.fromSealed[Suggestion]
}

object TextSuggestion {
  implicit val reads: Reads[TextSuggestion] = Json.reads[TextSuggestion]
  implicit val writes = new Writes[TextSuggestion] {
    def writes(suggestion: TextSuggestion) = Json.obj(
      "type" -> suggestion.`type`
    ) ++ Json.writes[TextSuggestion].writes(suggestion)
  }

  implicit val toTS = TSType.fromCaseClass[TextSuggestion]
}

case class TextSuggestion(text: String) extends Suggestion {
  val `type` = "TEXT_SUGGESTION"
}

