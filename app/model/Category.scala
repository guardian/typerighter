package model

import org.languagetool.rules.{Category => LTCategory}
import play.api.libs.json.{Json, Writes}

case class Category(id: String, name: String)

object Category {
  def fromLT(lt: LTCategory): Category = {
    Category(lt.getId.toString, lt.getName)
  }

  implicit val writes: Writes[Category] = Json.writes[Category]
}