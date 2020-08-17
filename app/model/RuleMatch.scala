package model

import org.languagetool.rules.{RuleMatch => LTRuleMatch}
import play.api.libs.json.{Json, Writes}

import scala.collection.JavaConverters._
import scala.util.matching.Regex

object RuleMatch {

  val surroundingBuffer = 100

  def surroundingText(text: String, from: Int, to: Int, buffer: Int = 0) = {

    val textBefore = text.substring(scala.math.max(from - buffer, 0), scala.math.max(from, 0))
    val textMatch = text.substring(from, to)
    val textAfter = text.substring(scala.math.min(to, text.length), scala.math.min(to + buffer, text.length))

    textBefore + "[" + textMatch + "]" + textAfter

  }

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

  def fromRegex(start: Int, end: Int, block: TextBlock, rule: RegexRule): RuleMatch = {
    val matchedText = block.text.substring(start, end)
    RuleMatch(
      rule = rule,
      fromPos = start + block.from,
      toPos = end + block.from,
      matchedText = matchedText,
      message = rule.description,
      shortMessage = Some(rule.description),
      suggestions = rule.suggestions,
      replacement = rule.replacement.map(replacement => rule.regex.replaceAllIn(matchedText, replacement.text)),
      markAsCorrect = rule.replacement.map(_.text).getOrElse("") == block.text.substring(start, end),
      matchContext = surroundingText(block.text, start, end, surroundingBuffer)
    )
  }


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

