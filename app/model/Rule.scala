package model

import java.net.URL

import org.languagetool.rules.{Rule => LTRule}
import play.api.libs.json.{Json, Writes}

import utils.JsonImplicits._

case class Rule(id: String, description: String, category: Category, url: Option[URL])

object Rule {
  def fromLT(lt: LTRule): Rule = {
    Rule(
      id = lt.getId,
      description = lt.getDescription,
      category = Category.fromLT(lt.getCategory),
      url = Option(lt.getUrl)
    )
  }

  implicit val writes: Writes[Rule] = Json.writes[Rule]
}
