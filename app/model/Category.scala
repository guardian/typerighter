package model

import org.languagetool.rules.{CategoryId, Category => LTCategory}
import play.api.libs.json.{Json, Writes, Reads}

import com.scalatsi.DefaultTSTypes._
import com.scalatsi.TSIType
import com.scalatsi.TSType
import com.scalatsi._

case class Category(id: String, name: String, colour: String)

/**
  * Categories define groups of Rules.
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

  implicit val toTS: TSIType[Category] = TSType.fromCaseClass[Category]
}
