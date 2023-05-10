package model

import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}

object PublishRuleForm {
  val form = Form(
    mapping(
      "reason" -> nonEmptyText()
    )(PublishRuleForm.apply)(PublishRuleForm.unapply)
  )
}

case class PublishRuleForm(
    reason: String
)
