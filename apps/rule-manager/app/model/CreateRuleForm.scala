package model

import play.api.data.Form
import play.api.data.Forms.{boolean, list, mapping, nonEmptyText, number, optional, text}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import service.RuleManager.RuleType

object CreateRuleForm {
  val ruleTypeConstraint: Constraint[String] = Constraint("constraints.ruleType")({ text =>
    val errors = text match {
      case RuleType.regex            => Nil
      case RuleType.languageToolCore => Nil
      case RuleType.languageToolXML  => Nil
      case RuleType.dictionary       => Nil
      case _ =>
        Seq(
          ValidationError(
            s"RuleType must be one of \"${RuleType.regex}\", \"${RuleType.languageToolXML}\", \"${RuleType.languageToolCore}\" or \"${RuleType.dictionary}\""
          )
        )
    }
    if (errors.isEmpty) {
      Valid
    } else {
      Invalid(errors)
    }
  })

  val form = Form(
    mapping(
      "ruleType" -> nonEmptyText().verifying(ruleTypeConstraint),
      "pattern" -> optional(text()),
      "replacement" -> optional(text()),
      "category" -> optional(text()),
      "tags" -> optional(list(number())),
      "description" -> optional(text()),
      "ignore" -> boolean,
      "notes" -> optional(text()),
      "forceRedRule" -> optional(boolean),
      "advisoryRule" -> optional(boolean),
      "externalId" -> optional(text())
    )(CreateRuleForm.apply)(CreateRuleForm.unapply)
  )
}

case class CreateRuleForm(
    ruleType: String,
    pattern: Option[String] = None,
    replacement: Option[String] = None,
    category: Option[String] = None,
    tags: Option[List[Int]] = None,
    description: Option[String] = None,
    ignore: Boolean,
    notes: Option[String] = None,
    forceRedRule: Option[Boolean] = None,
    advisoryRule: Option[Boolean] = None,
    externalId: Option[String] = None
)
