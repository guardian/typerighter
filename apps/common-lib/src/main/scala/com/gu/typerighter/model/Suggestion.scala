package com.gu.typerighter.model

import play.api.libs.json.{Json, Reads, Writes}

import scala.util.matching.Regex

object Suggestion {
  implicit val reads: Reads[Suggestion] = TextSuggestion.reads.reads
  implicit val writes: Writes[Suggestion] = { case textSuggestion: TextSuggestion =>
    TextSuggestion.writes.writes(textSuggestion)
  }
}

sealed trait Suggestion {
  val `type`: String
  val text: String

  /** If our suggestion is at the start of a sentence, cap up the first letter.
    */
  def ensureCorrectCase(isStartOfSentence: Boolean): Suggestion = this match {
    case TextSuggestion(text) if isStartOfSentence && text.nonEmpty =>
      TextSuggestion(text = s"${text.charAt(0).toUpper}${text.slice(1, text.length)}")
    case suggestion => suggestion
  }
}

object TextSuggestion {
  implicit val reads: Reads[TextSuggestion] = Json.reads[TextSuggestion]
  implicit val writes: Writes[TextSuggestion] = new Writes[TextSuggestion] {
    def writes(suggestion: TextSuggestion) = Json.obj(
      "type" -> suggestion.`type`
    ) ++ Json.writes[TextSuggestion].writes(suggestion)
  }
}

case class TextSuggestion(text: String) extends Suggestion {
  val `type` = "TEXT_SUGGESTION"

  def replaceAllIn(regex: Regex, matchedText: String) =
    this.copy(text = regex.replaceAllIn(matchedText, text))
}
