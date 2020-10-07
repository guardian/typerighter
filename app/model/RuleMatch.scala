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
    RuleMatch(
      rule = LTRule.fromLT(lt.getRule),
      fromPos = lt.getFromPos,
      toPos = lt.getToPos,
      matchedText = block.text.substring(lt.getFromPos, lt.getToPos),
      message = lt.getMessage,
      shortMessage = Some(lt.getMessage),
      replacement = replacement,
      suggestions = suggestions,
      matchContext = Text.getSurroundingText(block.text, lt.getFromPos, lt.getToPos),
      shortMatchContext = Text.getSurroundingText(block.text, lt.getFromPos, lt.getToPos, 50),
      matcherType = matcherType
    )
  }

  implicit val writes: Writes[RuleMatch] = Json.writes[RuleMatch]
}

case class RuleMatch(rule: BaseRule,
                     fromPos: Int,
                     toPos: Int,
                     matchedText: String,
                     message: String,
                     shortMessage: Option[String] = None,
                     suggestions: List[Suggestion] = List.empty,
                     replacement: Option[Suggestion] = None,
                     markAsCorrect: Boolean = false,
                     matchContext: String,
                     shortMatchContext: String,
                     matcherType: String)
