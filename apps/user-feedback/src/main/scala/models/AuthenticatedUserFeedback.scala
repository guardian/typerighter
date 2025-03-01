package models

import play.api.libs.json.{Format, Json}

/**
 * Feedback submitted to the user feedback API, with added user data from authentication.
 */
case class AuthenticatedUserFeedback(
  app: String,
  stage: String,
  documentUrl: String,
  feedbackMessage: String,
  userEmail: String,
  matchContext: Option[MatchContext]
)

object AuthenticatedUserFeedback {
  implicit val formats: Format[AuthenticatedUserFeedback] = Json.format[AuthenticatedUserFeedback]
}
