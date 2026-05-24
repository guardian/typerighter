package model

import play.api.libs.json.{Format, Json}

case class UserFeedbackAddressedForm(
    addressed: Boolean,
    notes: Option[String]
)

object UserFeedbackAddressedForm {
  implicit val format: Format[UserFeedbackAddressedForm] = Json.format[UserFeedbackAddressedForm]
}
