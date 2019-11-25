package model

import play.api.libs.json.{JsObject, Json}

/**
  * A rule to match text against.
  */
trait BaseRule {
  val id: String
  val category: Category
  val description: String
  val suggestions: List[Suggestion]
  val replacement: Option[TextSuggestion]
}

object BaseRule {
  def toJson(rule: BaseRule): JsObject = Json.obj(
    "id" -> rule.id,
    "category" -> rule.category,
    "description" -> rule.description,
    "suggestions" -> rule.suggestions,
    "replacement" -> rule.replacement
  )
}