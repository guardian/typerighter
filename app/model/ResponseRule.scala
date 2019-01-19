package model

import org.languagetool.rules.{Rule => LTRule}
import play.api.libs.json._

case class ResponseRule(id: String, description: String, category: Category, url: String)

/**
  * The subset of Rule data presented with rule matches when documents are checked.
  */
object ResponseRule {
  def fromLT(ltRule: LTRule): ResponseRule = {
    ResponseRule(
      id = ltRule.getId,
      description = ltRule.getDescription,
      category = Category.fromLT(ltRule.getCategory),
      url = if (ltRule.getUrl != null) ltRule.getUrl.toString else ""
    )
  }

  implicit val writes: Writes[ResponseRule] = Json.writes[ResponseRule]

  implicit val reads: Reads[ResponseRule] = Json.reads[ResponseRule]
}
