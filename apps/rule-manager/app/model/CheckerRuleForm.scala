package model

import com.gu.typerighter.model.{
  Category,
  ComparableRegex,
  LTRuleCore,
  LTRuleXML,
  RegexRule,
  TextSuggestion
}
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}

import scala.util.{Failure, Success, Try}
import scala.xml.XML

object RegexRuleForm {
  val regexConstraint: Constraint[String] = Constraint("constraints.regexcheck") { regexStr =>
    Try(regexStr.r) match {
      case Success(_) => Valid
      case Failure(exception) =>
        Invalid(
          Seq(ValidationError(s"Error parsing the regular expression: ${exception.getMessage()}"))
        )
    }
  }

  val form = Form(
    tuple(
      "pattern" -> nonEmptyText().verifying(regexConstraint),
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
  val xmlConstraint: Constraint[String] = Constraint("constraints.xmlcheck") { xmlStr =>
    Try(XML.loadString(xmlStr)) match {
      case Success(_) => Valid
      case Failure(exception) =>
        Invalid(Seq(ValidationError(s"Error parsing the XML: ${exception.getMessage()}")))
    }
  }

  val form = Form(
    tuple(
      "pattern" -> nonEmptyText().verifying(xmlConstraint),
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
