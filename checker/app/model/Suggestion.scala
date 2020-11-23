package model

import play.api.libs.json.{Json, Reads, Writes}
import scala.util.matching.Regex

object Suggestion {
  implicit val writes: Writes[Suggestion] = {
    case textSuggestion: TextSuggestion =>
      TextSuggestion.writes.writes(textSuggestion)
  }
}

sealed trait Suggestion {
  val `type`: String
  val text: String

  /**
    * If the first character of the suggestion is identical to the first character
    * of the matched text case, preserve the original casing. Used when our match covers
    * the start of a sentence to ensure we don't accidentally lowercase sentence starts.
    */
  def maybePreserveMatchCase(isStartOfSentence: Boolean, matchedText: String): Suggestion = this match {
    // A kludge to get around start-of-sentence casing. If the suggestion doesn't
    // match the whole matchedText, but does match the first character, preserve that
    // casing in the suggestion. This is to ensure that e.g. a case-insensitive suggestion
    // to replace e.g. 'end of sentence. [Mediavel]' with 'medieval' does not incorrectly replace
    // the uppercase 'M'.
    //
    // These sorts of rules are better off as dictionary matches, which we hope to add soon.
    case TextSuggestion(text) if isStartOfSentence => {
      TextSuggestion(text = matchedText.charAt(0).toUpper + text.slice(1, text.length))
    }
    case suggestion => suggestion
  }
}

object TextSuggestion {
  implicit val reads: Reads[TextSuggestion] = Json.reads[TextSuggestion]
  implicit val writes = new Writes[TextSuggestion] {
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
