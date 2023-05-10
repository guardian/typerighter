package model

import play.api.data.Form
import play.api.data.Forms.{boolean, mapping, optional, text}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import service.RuleManagement.RuleType

object UpdateRuleForm {
  val ruleTypeConstraint: Constraint[Option[String]] = Constraint("constraints.ruleType")({ text =>
    val errors = text match {
      case None                            => Nil
      case Some(RuleType.regex)            => Nil
      case Some(RuleType.languageToolCore) => Nil
      case Some(RuleType.languageToolXML)  => Nil
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
      "ruleType" -> optional(text()).verifying(ruleTypeConstraint),
      "pattern" -> optional(text()),
      "replacement" -> optional(text()),
      "category" -> optional(text()),
      "tags" -> optional(text()),
      "description" -> optional(text()),
      "advisoryRule" -> optional(boolean)
    )(UpdateRuleForm.apply)(UpdateRuleForm.unapply)
  )
}

case class UpdateRuleForm(
    ruleType: Option[String] = None,
    pattern: Option[String] = None,
    replacement: Option[String] = None,
    category: Option[String] = None,
    tags: Option[String] = None,
    description: Option[String] = None,
    advisoryRule: Option[Boolean] = None
)
