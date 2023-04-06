package model

import play.api.data.Form
import play.api.data.Forms.{boolean, mapping, nonEmptyText, number, optional, text}

object UpdateRuleForm {
  val form = Form(
    mapping(
      "id" -> number(),
      "ruleType" -> optional(text()),
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
    id: Int,
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
