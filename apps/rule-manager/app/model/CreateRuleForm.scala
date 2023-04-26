package model

import play.api.data.Form
import play.api.data.Forms.{boolean, mapping, nonEmptyText, optional, text}
import play.api.data.validation.{Constraint, ValidationError, Valid, Invalid}
import service.DbRuleManager.RuleType

object CreateRuleForm {
  val ruleTypeConstraint: Constraint[String] = Constraint("constraints.ruleType")({ text =>
    val errors = text match {
      case RuleType.regex            => Nil
      case RuleType.languageToolCore => Nil
      case RuleType.languageToolXML  => Nil
      case _ =>
        Seq(
          ValidationError(
            s"RuleType must be one of \"${RuleType.regex}\", \"${RuleType.languageToolXML}\" or \"${RuleType.languageToolCore}\""
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
      "tags" -> optional(text()),
      "description" -> optional(text()),
      "ignore" -> boolean,
      "notes" -> optional(text()),
      "forceRedRule" -> optional(boolean),
      "advisoryRule" -> optional(boolean)
    )(CreateRuleForm.apply)(CreateRuleForm.unapply)
  )
}

case class CreateRuleForm(
    ruleType: String,
    pattern: Option[String] = None,
    replacement: Option[String] = None,
    category: Option[String] = None,
    tags: Option[String] = None,
    description: Option[String] = None,
    ignore: Boolean,
    notes: Option[String] = None,
    forceRedRule: Option[Boolean] = None,
    advisoryRule: Option[Boolean] = None
)
