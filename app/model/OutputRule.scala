package model

import org.languagetool.rules.{Rule => LTRule}
import play.api.libs.json._

case class OutputRule(id: String, description: String, category: Category, url: String)

/**
  * The subset of Rule data presented with rule matches when documents are checked.
  */
object OutputRule {
  def fromLT(ltRule: LTRule): OutputRule = {
    OutputRule(
      id = ltRule.getId,
      description = ltRule.getDescription,
      category = Category.fromLT(ltRule.getCategory),
      url = if (ltRule.getUrl != null) ltRule.getUrl.toString else ""
    )
  }

  implicit val writes: Writes[OutputRule] = Json.writes[OutputRule]

  implicit val reads: Reads[OutputRule] = Json.reads[OutputRule]
}
