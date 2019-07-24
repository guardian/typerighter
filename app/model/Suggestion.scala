package model

import play.api.libs.json.{Json, Writes}

object Suggestion {
  implicit val writes: Writes[Suggestion] = {
    case baseSuggestion: BaseSuggestion =>
      BaseSuggestion.writes.writes(baseSuggestion)
    case wikiSuggestion: WikiSuggestion =>
      WikiSuggestion.writes.writes(wikiSuggestion)
  }
}

sealed trait Suggestion {
  def `type`: String
}

object BaseSuggestion {
  implicit val writes: Writes[BaseSuggestion] = Json.writes[BaseSuggestion]
}
case class BaseSuggestion(replacement: String) extends Suggestion {
  def `type` = "BASE_SUGGESTION"
}

object WikiAbstract {
  implicit val writes: Writes[WikiAbstract] = Json.writes[WikiAbstract]
}
case class WikiAbstract(
    title: String,
    description: String,
    thumbnail: String,
    relevance: Long
)

object WikiSuggestion {
  implicit val writes: Writes[WikiSuggestion] = Json.writes[WikiSuggestion]
}
case class WikiSuggestion(
    matches: List[WikiAbstract]
) extends Suggestion {
  def `type` = "WIKI_SUGGESTION"
}
