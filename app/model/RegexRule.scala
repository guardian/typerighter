package model

import play.api.libs.json.{JsPath, JsResult, JsString, JsSuccess, JsValue, Json, Reads, Writes}

import scala.util.matching.Regex
import utils.Text
import matchers.RegexMatcher

object RegexRule {
  implicit val regexWrites: Writes[Regex] = (regex: Regex) => JsString(regex.toString)
  implicit val regexReads: Reads[Regex] = (JsPath).read[String].map(new Regex(_))
  implicit val writes: Writes[RegexRule] = Json.writes[RegexRule]
  implicit val reads: Reads[RegexRule] = Json.reads[RegexRule]
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
      replacement = replacement.map(_.replaceAllIn(regex, matchedText)),
      markAsCorrect = replacement.map(_.text).getOrElse("") == block.text.substring(start, end),
      matchContext = Text.getSurroundingText(block.text, start, end),
      matcherType = RegexMatcher.getType
    )
  }
}
