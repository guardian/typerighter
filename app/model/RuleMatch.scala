package model

import org.languagetool.rules.{RuleMatch => LTRuleMatch}
import play.api.libs.json.{Json, Reads, Writes}

import scala.collection.JavaConverters._

case class RuleMatch(
    rule: ResponseRule,
    fromPos: Int,
    toPos: Int,
    message: String,
    shortMessage: Option[String] = None,
    suggestedReplacements: List[Suggestion] = List.empty
)

object RuleMatch {
  def fromLT(lt: LTRuleMatch): RuleMatch = {
    RuleMatch(
      rule = ResponseRule.fromLT(lt.getRule),
      fromPos = lt.getFromPos,
      toPos = lt.getToPos,
      message = lt.getMessage,
      shortMessage = Some(lt.getMessage),
      suggestedReplacements = lt.getSuggestedReplacements.asScala.toList.map {
        BaseSuggestion(_)
      }
    )
  }

  implicit val reads: Reads[RuleMatch] = Json.reads[RuleMatch]
  implicit val writes: Writes[RuleMatch] = Json.writes[RuleMatch]
}
