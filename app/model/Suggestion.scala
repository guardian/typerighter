package model

import play.api.libs.json.{Json, Writes}

object Suggestion {
  implicit val writes: Writes[Suggestion] = {
    case textSuggestion: TextSuggestion =>
      TextSuggestion.writes.writes(textSuggestion)
    case wikiSuggestion: WikiSuggestion =>
      WikiSuggestion.writes.writes(wikiSuggestion)
  }
}

sealed trait Suggestion {
  val `type`: String
  val text: String
}

object TextSuggestion {
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

case class WikiAbstract(
    title: String,
    description: String,
    thumbnail: String,
    relevance: Long
)

object WikiSuggestion {
  implicit val writes = new Writes[WikiSuggestion] {
    def writes(wikiArticle: WikiSuggestion) = Json.obj(
      "type" -> wikiArticle.`type`,
      "text" -> wikiArticle.text,
      "title" -> wikiArticle.title,
      "score" -> wikiArticle.score
    )
  }
}

case class WikiSuggestion(
    text: String,
    title: String,
    score: Double
) extends Suggestion {
  val `type` = "WIKI_SUGGESTION"
}

