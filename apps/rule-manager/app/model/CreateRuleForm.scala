package model

import play.api.data.Form
import play.api.data.Forms.{boolean, mapping, optional, text}

object CreateRuleForm {
  val form = Form(
    mapping(
      "ruleType" -> text(),
      "pattern" -> optional(text()),
      "replacement" -> optional(text()),
      "category" -> optional(text()),
      "tags" -> optional(text()),
      "description" -> optional(text()),
      "ignore" -> boolean,
      "notes" -> optional(text()),
      "googleSheetId" -> optional(text()),
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
  googleSheetId: Option[String] = None,
  forceRedRule: Option[Boolean] = None,
  advisoryRule: Option[Boolean] = None
)