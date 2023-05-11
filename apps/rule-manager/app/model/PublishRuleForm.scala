package model

import play.api.data.Form
import play.api.data.Forms.{nonEmptyText, single}

object PublishRuleForm {
  val form = Form(
    single(
      "reason" -> nonEmptyText()
    )
  )
}
