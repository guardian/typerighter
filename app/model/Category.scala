package model

import org.languagetool.rules.{CategoryId, Category => LTCategory}
import play.api.libs.json.{Json, Writes, Reads}

case class Category(id: String, name: String, colour: String)

/**
  * The application's representation of a LanguageTool Category.
  *
  * We use tabName as a temporary place to store colour information.
  */
object Category {
  def fromLT(lt: LTCategory): Category = {
    Category(lt.getId.toString, lt.getName, lt.getTabName)
  }

  def toLT(category: Category): LTCategory = {
    new LTCategory(new CategoryId(category.id), category.name, LTCategory.Location.EXTERNAL, true, category.colour)
  }

  implicit val writes: Writes[Category] = Json.writes[Category]

  implicit val reads: Reads[Category] = Json.reads[Category]
}