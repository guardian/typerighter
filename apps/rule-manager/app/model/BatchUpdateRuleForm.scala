package model

import play.api.data._
import play.api.data.Forms._

object BatchUpdateRuleForm {
  val form = Form(
    mapping(
      "ids" -> list(number),
      "fields" -> mapping(
        "category" -> text,
        "tags" -> text
      )(BatchUpdateFields.apply)(BatchUpdateFields.unapply)
    )(BatchUpdateRuleForm.apply)(BatchUpdateRuleForm.unapply)
  )
}

case class BatchUpdateRuleForm(ids: List[Int], fields: BatchUpdateFields)
case class BatchUpdateFields(category: String, tags: String)
