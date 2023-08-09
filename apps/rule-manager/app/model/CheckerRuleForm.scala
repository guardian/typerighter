package model

import com.gu.typerighter.model.{
  Category,
  ComparableRegex,
  LTRuleCore,
  LTRuleXML,
  RegexRule,
  TextSuggestion,
  DictionaryRule
}
import play.api.data.Form
import play.api.data.Forms._

object RegexRuleForm {
  val form = Form(
    tuple(
      "pattern" -> nonEmptyText(),
      "replacement" -> optional(text()),
      "category" -> nonEmptyText(),
      "description" -> nonEmptyText(),
      "externalId" -> nonEmptyText()
    )
  )

  def toRegexRule(
      pattern: String,
      replacement: Option[String],
      category: String,
      description: String,
      externalId: String
  ) = {
    RegexRule(
      id = externalId,
      category = Category(id = category, name = category),
      description = description,
      suggestions = List.empty,
      replacement = replacement.map(TextSuggestion(_)),
      regex = new ComparableRegex(pattern)
    )
  }
}

object LTRuleXMLForm {
  val form = Form(
    tuple(
      "pattern" -> nonEmptyText(),
      "category" -> nonEmptyText(),
      "description" -> nonEmptyText(),
      "externalId" -> nonEmptyText()
    )
  )

  def toLTRuleXML(pattern: String, category: String, description: String, externalId: String) = {
    LTRuleXML(
      id = externalId,
      category = Category(id = category, name = category),
      description = description,
      xml = pattern
    )
  }
}

object LTRuleCoreForm {
  val form = Form(
    single(
      "externalId" -> nonEmptyText()
    )
  )

  def toLTRuleCore(externalId: String) = {
    LTRuleCore(
      id = externalId,
      languageToolRuleId = externalId
    )
  }
}

object DictionaryForm {
  val form = Form(
    tuple(
      "pattern" -> nonEmptyText(),
      "category" -> nonEmptyText(),
      "externalId" -> nonEmptyText()
    )
  )

  def toDictionary(pattern: String, externalId: String, category: String) = {
    DictionaryRule(
      id = externalId,
      word = pattern,
      category = Category(id = category, name = category)
    )
  }
}
