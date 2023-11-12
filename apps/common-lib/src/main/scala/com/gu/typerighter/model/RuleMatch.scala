package com.gu.typerighter.model

import org.languagetool.rules.{RuleMatch => LTRuleMatch}
import play.api.libs.json.{JsObject, JsString, Json, Reads, Writes}

import scala.jdk.CollectionConverters._

object RuleMatch {
  def fromLT(lt: LTRuleMatch, block: TextBlock): RuleMatch = {
    val suggestions = lt.getSuggestedReplacements.asScala.toList.map {
      TextSuggestion(_)
    }
    // If a rule has one or more suggestions, trigger the 'replacement' behaviour in the client.
    val replacement = suggestions.headOption
    val matchedText = block.text.substring(lt.getFromPos, lt.getToPos)
    val (precedingText, subsequentText) =
      Text.getSurroundingText(block.text, lt.getFromPos, lt.getToPos)
    RuleMatch(
      rule = LTRule.fromLT(lt.getRule),
      fromPos = lt.getFromPos,
      toPos = lt.getToPos,
      precedingText = precedingText,
      subsequentText = subsequentText,
      matchedText = matchedText,
      message = lt.getMessage,
      shortMessage = Some(lt.getMessage),
      replacement = replacement,
      suggestions = suggestions,
      matchContext = Text.getMatchTextSnippet(precedingText, matchedText, subsequentText)
    )
  }

  implicit val reads: Reads[RuleMatch] = Json.reads[RuleMatch]

  // Overwrite the default `groupKey` with the derivedGroupKey on serialisation
  val defaultWrites: Writes[RuleMatch] = Json.writes[RuleMatch]
  implicit val writes: Writes[RuleMatch] = (ruleMatch: RuleMatch) => {
    defaultWrites.writes(ruleMatch) match {
      case obj: JsObject => obj ++ JsObject(Seq("groupKey" -> JsString(ruleMatch.derivedGroupKey)))
      case _             => throw new Error("Serialised to incorrect type")
    }
  }
}

case class RuleMatch(
    rule: CheckerRule,
    fromPos: Int,
    toPos: Int,
    precedingText: String,
    subsequentText: String,
    matchedText: String,
    message: String,
    shortMessage: Option[String] = None,
    suggestions: List[Suggestion] = List.empty,
    replacement: Option[Suggestion] = None,
    markAsCorrect: Boolean = false,
    matchContext: String,
    groupKey: Option[String] = None,
    priority: Int = 0
) {
  val derivedGroupKey = groupKey.getOrElse(rule.id)
}
