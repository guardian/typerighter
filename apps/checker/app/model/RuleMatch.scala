package model

import org.languagetool.rules.{RuleMatch => LTRuleMatch}
import play.api.libs.json.{Json, Writes}

import scala.jdk.CollectionConverters._
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
    val (precedingText, subsequentText) = Text.getSurroundingText(block.text, lt.getFromPos, lt.getToPos)
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
      matchContext = Text.getMatchTextSnippet(precedingText, matchedText, subsequentText),
      matcherType = matcherType
    )
  }

  implicit val writes: Writes[RuleMatch] = Json.writes[RuleMatch]
}

case class RuleMatch(rule: BaseRule,
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
                     matcherType: String) {
  /**
    * Map the range this match applies to through the given ranges, adjusting its range accordingly.
    */
  def mapThroughSkippedRanges(skipRanges: List[TextRange]): RuleMatch = skipRanges match {
    case Nil => this
    case skipRanges => {
      val (newMatch, _) = skipRanges.foldLeft((this, List.empty[TextRange]))((acc, range) => acc match {
        case (ruleMatch, rangesAlreadyApplied) => {
          val newMatchRange = TextRange(ruleMatch.fromPos, ruleMatch.toPos).mapAddedRange(range)
          val newRuleMatch = ruleMatch.copy(fromPos = newMatchRange.from, toPos = newMatchRange.to)

          (newRuleMatch, rangesAlreadyApplied)
        }
      })

      newMatch
    }
  }

  /**
    * Map this match through the given blocks' skipped ranges.
    */
  def mapMatchThroughBlocks(blocks: List[TextBlock]): RuleMatch = {
    val maybeBlockForThisMatch = blocks.find(block => fromPos >= block.from  && toPos <= block.to)
    val skipRangesForThisBlock = maybeBlockForThisMatch.flatMap(_.skipRanges).getOrElse(Nil)
    mapThroughSkippedRanges(skipRangesForThisBlock)
  }
}

