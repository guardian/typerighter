
package com.gu.typerighter.model

import play.api.libs.json.{Json, Reads, Writes}
import org.languagetool.rules.{CategoryId, Category => LTCategory}

case class Category(id: String, name: String)

/**
  * The application's representation of a LanguageTool Category.
  */
object Category {
  def fromLT(lt: LTCategory): Category = {
    Category(lt.getId.toString, lt.getName)
  }

  def toLT(category: Category): LTCategory = {
    new LTCategory(new CategoryId(category.id), category.name, LTCategory.Location.EXTERNAL, true)
  }

  implicit val writes: Writes[Category] = Json.writes[Category]

  implicit val reads: Reads[Category] = Json.reads[Category]
}
