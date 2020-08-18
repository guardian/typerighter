package model

import play.api.libs.json.{JsString, Json, Writes}

import scala.util.matching.Regex
import utils.Text

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
) extends BaseRule {

  def toMatch(start: Int, end: Int, block: TextBlock): RuleMatch = {
    val matchedText = block.text.substring(start, end)
    RuleMatch(
      rule = this,
      fromPos = start + block.from,
      toPos = end + block.from,
      matchedText = matchedText,
      message = description,
      shortMessage = Some(description),
      suggestions = suggestions,
      replacement = replacement.map(replacement => regex.replaceAllIn(matchedText, replacement.text)),
      markAsCorrect = replacement.map(_.text).getOrElse("") == block.text.substring(start, end),
      matchContext = Text.getSurroundingText(block.text, start, end)
    )
  }
}
