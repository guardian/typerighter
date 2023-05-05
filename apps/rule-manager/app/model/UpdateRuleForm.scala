package model

import play.api.data.Form
import play.api.data.Forms.{boolean, mapping, optional, text}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import service.DbRuleManager.RuleType

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
      "ignore" -> optional(boolean),
      "notes" -> optional(text()),
      "googleSheetId" -> optional(text()),
      "forceRedRule" -> optional(boolean),
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
    ignore: Option[Boolean] = None,
    notes: Option[String] = None,
    googleSheetId: Option[String] = None,
    forceRedRule: Option[Boolean] = None,
    advisoryRule: Option[Boolean] = None
)
