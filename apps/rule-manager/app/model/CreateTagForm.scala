package model

import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}

object CreateTagForm {
  val form = Form(
    mapping(
      "name" -> nonEmptyText()
    )(CreateTagForm.apply)(CreateTagForm.unapply)
  )
}
case class CreateTagForm(
    name: String
)
