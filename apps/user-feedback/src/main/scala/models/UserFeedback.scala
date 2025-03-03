package models

import com.gu.pandomainauth.model.User
import play.api.libs.json.{Format, Json}

/** Feedback submitted to the user feedback API.
  */
case class UserFeedback(
    app: String,
    stage: String,
    documentUrl: String,
    feedbackMessage: String,
    matchContext: Option[MatchContext] = None
) {
  def withUser(user: User): AuthenticatedUserFeedback = AuthenticatedUserFeedback(
    app,
    stage,
    documentUrl,
    feedbackMessage,
    user.email,
    matchContext
  )
}

object UserFeedback {
  implicit val formats: Format[UserFeedback] = Json.format[UserFeedback]
}
