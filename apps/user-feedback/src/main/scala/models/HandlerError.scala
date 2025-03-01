package models

import com.gu.pandomainauth.model.AuthenticationStatus
import play.api.libs.json.JsError

/**
 * Encapsulates all of the unhappy paths for the lambda handler.
 */
sealed trait HandlerError

case class CookiesNotFoundError() extends HandlerError
case class PanDomainAuthStatusError(authStatus: AuthenticationStatus) extends HandlerError
case class JsonParseError(message: String) extends HandlerError
case class JsonValidateError(jsError: JsError) extends HandlerError
case class SendingError(error: Throwable) extends HandlerError