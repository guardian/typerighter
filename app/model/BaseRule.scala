package model

import play.api.libs.json.{JsObject, Json}

/**
  * A rule to match text against.
  */
trait BaseRule {
  val surroundingBuffer = 100

  def surroundingText(text: String, from: Int, to: Int, buffer: Int = 0) = {

    val textBefore = text.substring(scala.math.max(from - buffer, 0), scala.math.max(from, 0))
    val textMatch = text.substring(from, to)
    val textAfter = text.substring(scala.math.min(to, text.length), scala.math.min(to + buffer, text.length))

    textBefore + "[" + textMatch + "]" + textAfter

  }

  val id: String
  val category: Category
  val description: String
  val suggestions: List[Suggestion]
  val replacement: Option[TextSuggestion]

  /**
   * Given a block and a match, translate this rule into a RuleMatch.
   *
   * @param {Int} the from position
   * @param {Int} the to position
   * @param {TextBlock} the Textblock this rule is matching
   */
  def toMatch: (Int, Int, TextBlock) => RuleMatch
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
