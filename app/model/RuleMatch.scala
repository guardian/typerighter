package model

import org.languagetool.rules.{RuleMatch => LTRuleMatch}
import play.api.libs.json.{Json, Writes}

import scala.collection.JavaConverters._

case class RuleMatch(rule: Rule,
                     fromPos: Int,
                     toPos: Int,
                     message: String,
                     shortMessage: Option[String],
                     suggestedReplacements: List[String])

object RuleMatch {
  def fromLT(lt: LTRuleMatch): RuleMatch = {
    RuleMatch(
      rule = Rule.fromLT(lt.getRule),
      fromPos = lt.getFromPos,
      toPos = lt.getToPos,
      message = lt.getMessage,
      shortMessage = Option(lt.getShortMessage).filterNot(_.isEmpty),
      suggestedReplacements = lt.getSuggestedReplacements.asScala.toList
    )
  }

  implicit val writes: Writes[RuleMatch] = Json.writes[RuleMatch]
}