package model

import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}

object TagForm {
  val form = Form(
    mapping(
      "name" -> nonEmptyText()
    )(TagForm.apply)(TagForm.unapply)
  )
}
case class TagForm(
    name: String
)
