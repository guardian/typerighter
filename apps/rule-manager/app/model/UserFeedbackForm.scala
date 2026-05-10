package models

import play.api.libs.json.{Format, Json}
import play.api.data.Forms.{boolean, mapping, optional, text}
import play.api.data.Form

/** Feedback submitted to the user feedback API
  */
case class UserFeedback(
    app: String,
    stage: String,
    documentUrl: String,
    feedbackMessage: String,
    matchContext: Option[MatchContext]
)

object UserFeedback {
  val form = Form(
    mapping(
      "app" -> text(),
      "stage" -> text(),
      "documentUrl" -> text(),
      "feedbackMessage" -> text(),
      "matchContext" -> optional(
        mapping(
          "matchId" -> text(),
          "ruleId" -> text(),
          "documentId" -> text(),
          "matcherType" -> text(),
          "suggestion" -> optional(text()),
          "matchIsMarkedAsCorrect" -> boolean,
          "matchIsAdvisory" -> boolean,
          "matchHasReplacement" -> boolean,
          "matchedText" -> text(),
          "matchContext" -> text()
        )(MatchContext.apply)(MatchContext.unapply)
      )
    )(UserFeedback.apply)(UserFeedback.unapply)
  )
}

/** Feedback submitted to the user feedback API, with added user data from authentication.
  */
case class UserFeedbackWithAuthentication(
    app: String,
    stage: String,
    documentUrl: String,
    feedbackMessage: String,
    userEmail: String,
    matchContext: Option[MatchContext]
)

object UserFeedbackWithAuthentication {
  implicit val formats: Format[UserFeedbackWithAuthentication] =
    Json.format[UserFeedbackWithAuthentication]

  def fromUserFeedback(userFeedback: UserFeedback, userEmail: String) =
    UserFeedbackWithAuthentication(
      app = userFeedback.app,
      stage = userFeedback.stage,
      documentUrl = userFeedback.documentUrl,
      feedbackMessage = userFeedback.feedbackMessage,
      userEmail = userEmail,
      matchContext = userFeedback.matchContext
    )
}

/** When feedback is given in the context of a match, we capture additional information about that
  * context here.
  */
case class MatchContext(
    matchId: String,
    ruleId: String,
    documentId: String,
    matcherType: String,
    suggestion: Option[String],
    matchIsMarkedAsCorrect: Boolean,
    matchIsAdvisory: Boolean,
    matchHasReplacement: Boolean,
    matchedText: String,
    matchContext: String
)

object MatchContext {
  implicit val formats: Format[MatchContext] = Json.format[MatchContext]
}
