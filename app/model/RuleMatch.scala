package model

import org.languagetool.rules.{RuleMatch => LTRuleMatch}
import play.api.libs.json.{Json, Writes}

import scala.collection.JavaConverters._
import scala.util.matching.Regex

import utils.Text

object RuleMatch {
  def fromLT(lt: LTRuleMatch, block: TextBlock, matcherType: String): RuleMatch = {
    val suggestions = lt.getSuggestedReplacements.asScala.toList.map {
      TextSuggestion(_)
    }
    // Placeholder rule-of-thumb: if a rule has exactly one suggestion, add
    // it as a replacement to trigger the 'replacement' behaviour in the client.
    val replacement = if (suggestions.size == 1) Some(suggestions.head) else None
    val matchedText = block.text.substring(lt.getFromPos, lt.getToPos)
    val (before, after) = Text.getSurroundingText(block.text, lt.getFromPos, lt.getToPos, 50)
    RuleMatch(
      rule = LTRule.fromLT(lt.getRule),
      fromPos = lt.getFromPos,
      toPos = lt.getToPos,
      before = before,
      after = after,
      matchedText = matchedText,
      message = lt.getMessage,
      shortMessage = Some(lt.getMessage),
      replacement = replacement,
      suggestions = suggestions,
      matchContext = Text.getMatchTextSnippet(before, matchedText, after),
      matcherType = matcherType
    )
  }

  implicit val writes: Writes[RuleMatch] = Json.writes[RuleMatch]
}

case class RuleMatch(rule: BaseRule,
                     fromPos: Int,
                     toPos: Int,
                     before: String,
                     after: String,
                     matchedText: String,
                     message: String,
                     shortMessage: Option[String] = None,
                     suggestions: List[Suggestion] = List.empty,
                     replacement: Option[Suggestion] = None,
                     markAsCorrect: Boolean = false,
                     matchContext: String,
                     matcherType: String)
