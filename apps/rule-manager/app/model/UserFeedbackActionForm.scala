package model

import play.api.libs.json.{Format, Json}

case class UserFeedbackActionForm(
    actioned: Boolean,
    actionType: String,
    actionNotes: Option[String]
)

object UserFeedbackActionForm {
  implicit val format: Format[UserFeedbackActionForm] = Json.format[UserFeedbackActionForm]
}
