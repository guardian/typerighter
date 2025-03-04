import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.amazonaws.services.lambda.runtime.events.{
  APIGatewayProxyRequestEvent,
  APIGatewayProxyResponseEvent
}
import com.gu.pandomainauth.model.{
  Authenticated,
  AuthenticationStatus,
  Expired,
  GracePeriod,
  InvalidCookie,
  NotAuthenticated,
  NotAuthorized
}
import models.{
  CookiesNotFoundError,
  JsonParseError,
  JsonValidateError,
  PanDomainAuthStatusError,
  SendingError,
  UserFeedback
}
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json._
import utils.UserFeedbackDependencies

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

class Handler
    extends RequestHandler[APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent]
    with UserFeedbackDependencies {

  override def handleRequest(
      input: APIGatewayProxyRequestEvent,
      context: Context
  ): APIGatewayProxyResponseEvent = {
    val logger = context.getLogger
    logger.log(s"Received event: ${input.getBody}")

    val result = for {
      cookie <- input.getHeaders.asScala.get("Cookie").toRight(CookiesNotFoundError())
      _ = logger.log("Got cookie, authenticating")
      user <- authenticateRequest(cookie)
      _ = logger.log(s"Authenticated with user $user")
      userFeedbackJson <- Try(Json.parse(input.getBody)).toEither.left.map(e =>
        JsonParseError(e.getMessage)
      )
      _ = logger.log(s"Parsed JSON for user feedback: ${userFeedbackJson.toString}")
      userFeedback <- userFeedbackJson.validate[UserFeedback] match {
        case JsSuccess(userFeedback, _) =>
          Right(userFeedback)
        case jsError @ JsError(_) => Left(JsonValidateError(jsError))
      }
      _ = logger.log(s"Validated user feedback: ${userFeedback.toString}")
      authenticatedUserFeedback = userFeedback.withUser(user)
      _ = logger.log(
        s"Sending user feedback with additional user information: ${authenticatedUserFeedback.toString}"
      )
      _ <- Try(
        snsEventSender.sendEvent(
          config.userFeedbackSnsTopic,
          authenticatedUserFeedback.toString
        )
      ) match {
        case Success(_) => Right(authenticatedUserFeedback)
        case Failure(e) => Left(SendingError(e))
      }
    } yield {
      logger.log(s"Successfully sent user feedback $authenticatedUserFeedback")
      authenticatedUserFeedback
    }

    val response = new APIGatewayProxyResponseEvent()

    val (statusCode, body) = result match {
      case Right(userFeedback) =>
        200 -> Json.obj(
          "status" -> "success",
          "message" -> "User feedback processed successfully",
          "userFeedback" -> userFeedback
        )
      case Left(SendingError(e)) =>
        logger.log(s"Error sending message: $e")
        500 -> getErrorResponse("Error sending message")
      case Left(CookiesNotFoundError()) =>
        400 -> getErrorResponse("No cookies found in header")
      case Left(PanDomainAuthStatusError(authStatus: AuthenticationStatus)) =>
        authStatus match {
          case Expired(_) =>
            401 -> getErrorResponse("Authentication expired")
          case NotAuthorized(_) =>
            403 -> getErrorResponse("Unauthorised")
          case InvalidCookie(e) =>
            logger.log(s"Invalid cookie: $e")
            400 -> getErrorResponse("Invalid cookie")
          case NotAuthenticated =>
            401 -> getErrorResponse("Not authenticated")
          case _ =>
            logger.log(s"Received $authStatus as a Left. This shouldn't happen!")
            500 -> getErrorResponse("Unexpected error authenticating")
        }
      case Left(JsonParseError(message: String)) =>
        400 -> getErrorResponse(s"Error parsing JSON: $message")
      case Left(JsonValidateError(jsError: JsError)) =>
        val errorDetail = jsError.errors.map { case (path, errors) =>
          path.toJsonString -> errors.map(_.message)
        }.toMap
        400 -> getErrorResponse(s"Error validating JSON: $errorDetail")
    }

    response.setStatusCode(statusCode)
    response.setBody(body.toString)

    response
  }

  private def getErrorResponse(message: String) = Json.obj(
    "status" -> "error",
    "message" -> message
  )

  private def authenticateRequest(cookie: String) =
    auth.authenticateCookie(cookie, config.appName, config.publicSettingsVerification) match {
      case Authenticated(authedUser)     => Right(authedUser.user)
      case GracePeriod(authedUser)       => Right(authedUser.user)
      case authStatus @ Expired(_)       => Left(PanDomainAuthStatusError(authStatus))
      case authStatus @ NotAuthorized(_) => Left(PanDomainAuthStatusError(authStatus))
      case authStatus @ NotAuthenticated => Left(PanDomainAuthStatusError(authStatus))
      case authStatus @ InvalidCookie(_) => Left(PanDomainAuthStatusError(authStatus))
    }
}
