package model

import org.languagetool.rules.{RuleMatch => LTRuleMatch}
import play.api.libs.json.{Json, Writes}

import scala.collection.JavaConverters._
import scala.util.matching.Regex

object RuleMatch {
  def fromLT(lt: LTRuleMatch, block: TextBlock): RuleMatch = RuleMatch(
      rule = LTRule.fromLT(lt.getRule),
      fromPos = lt.getFromPos,
      toPos = lt.getToPos,
      matchedText = block.text.substring(lt.getFromPos, lt.getToPos),
      message = lt.getMessage,
      shortMessage = Some(lt.getMessage),
      suggestions = lt.getSuggestedReplacements.asScala.toList.map {
        TextSuggestion(_)
      },
      matchContext = surroundingText(block.text, lt.getFromPos, lt.getToPos, surroundingBuffer)
    )

  implicit val writes: Writes[RuleMatch] = Writes[RuleMatch]((ruleMatch: RuleMatch) => Json.obj(
    "rule" -> BaseRule.toJson(ruleMatch.rule),
    "fromPos" -> ruleMatch.fromPos,
    "toPos" -> ruleMatch.toPos,
    "matchedText" -> ruleMatch.matchedText,
    "message" -> ruleMatch.message,
    "shortMessage" -> ruleMatch.shortMessage,
    "suggestions" -> ruleMatch.suggestions,
    "markAsCorrect" -> ruleMatch.markAsCorrect,
    "matchContext" -> ruleMatch.matchContext
  ))
}

case class RuleMatch(rule: BaseRule,
                     fromPos: Int,
                     toPos: Int,
                     matchedText: String,
                     message: String,
                     shortMessage: Option[String] = None,
                     suggestions: List[Suggestion] = List.empty,
                     replacement: Option[String] = None,
                     markAsCorrect: Boolean = false,
                     matchContext: String)

