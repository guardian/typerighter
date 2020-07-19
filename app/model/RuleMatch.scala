package model

import org.languagetool.rules.{RuleMatch => LTRuleMatch}
import play.api.libs.json.{Json, Writes}

import scala.collection.JavaConverters._

object RuleMatch {
  def fromLT(lt: LTRuleMatch, block: TextBlock): RuleMatch = {
    RuleMatch(
      rule = LTRule.fromLT(lt.getRule),
      fromPos = lt.getFromPos,
      toPos = lt.getToPos,
      matchedText = block.text.substring(lt.getFromPos, lt.getToPos),
      message = lt.getMessage,
      shortMessage = Some(lt.getMessage),
      suggestions = lt.getSuggestedReplacements.asScala.toList.map { TextSuggestion(_) }
    )
  }

  implicit val writes = Json.writes[RuleMatch]
}

case class RuleMatch(rule: BaseRule,
                     fromPos: Int,
                     toPos: Int,
                     matchedText: String,
                     message: String,
                     shortMessage: Option[String] = None,
                     suggestions: List[Suggestion] = List.empty,
                     markAsCorrect: Boolean = false)

