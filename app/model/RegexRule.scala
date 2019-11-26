package model

import play.api.libs.json.{JsString, Json, Writes}

import scala.util.matching.Regex

object RegexRule {
  implicit val regexWrites: Writes[Regex] = (regex: Regex) => JsString(regex.toString)
  implicit val writes: Writes[RegexRule] = Json.writes[RegexRule]
}

case class RegexRule(
    id: String,
    category: Category,
    description: String,
    suggestions: List[TextSuggestion] = List.empty,
    replacement: Option[TextSuggestion] = None,
    regex: Regex
) extends BaseRule
