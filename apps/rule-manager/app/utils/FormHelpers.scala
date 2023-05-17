package utils

import play.api.data.FormError
import play.api.libs.json.{JsValue, Json, Writes}

case class FormErrorEnvelope(errors: Seq[FormError])

trait FormHelpers {
  implicit object FormErrorWrites extends Writes[FormError] {
    override def writes(o: FormError): JsValue = Json.obj(
      "key" -> Json.toJson(o.key),
      "message" -> Json.toJson(o.message.replace("error.", ""))
    )
  }

  def toFormError(key: String)(errors: Seq[String]): Seq[FormError] =
    errors.map(e => formErrorFromString(key)(e))

  def formErrorFromString(key: String)(message: String): FormError = FormError(key, message)
}

object FormErrorEnvelope extends FormHelpers {
  implicit val writes: Writes[FormErrorEnvelope] = Json.writes[FormErrorEnvelope]
}
