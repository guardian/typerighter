package models

import play.api.libs.json.{Format, Json}
import play.api.data.Forms.{boolean, mapping, optional, text}
import play.api.data.Form

/** Feedback submitted to the user feedback API
  */
case class UserFeedback(
    // The application the feedback came from
    app: String,
    // The application stage
    stage: String,
    // The URL of the document the submission relates to
    documentUrl: String,
    // The message from the user
    feedbackMessage: String,
    // Additional document and match context if the feedback relates to a specific match
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

/** When feedback is given in the context of a match, we capture additional information about that
  * context here.
  */
case class MatchContext(
    // The id of the match, unique to the check
    matchId: String,
    // The rule's external ID
    ruleId: String,
    // The ID of the document that produced the match
    documentId: String,
    // The type of matcher that produced the rule - see [[checker.utils.Matcher]]
    matcherType: String,
    // The suggestion that was provided, if any
    suggestion: Option[String],
    matchIsMarkedAsCorrect: Boolean,
    matchIsAdvisory: Boolean,
    matchHasReplacement: Boolean,
    matchedText: String,
    // A snippet of text showing the match and a small amount of surrounding text
    matchContext: String
)

object MatchContext {
  implicit val formats: Format[MatchContext] = Json.format[MatchContext]
}

/** Feedback submitted to the user feedback API, with the e-mail of the submitter.
  */
case class UserFeedbackWithEmail(
    app: String,
    stage: String,
    documentUrl: String,
    feedbackMessage: String,
    userEmail: String,
    matchContext: Option[MatchContext]
)

object UserFeedbackWithEmail {
  implicit val formats: Format[UserFeedbackWithEmail] =
    Json.format[UserFeedbackWithEmail]

  def fromUserFeedback(userFeedback: UserFeedback, userEmail: String) =
    UserFeedbackWithEmail(
      app = userFeedback.app,
      stage = userFeedback.stage,
      documentUrl = userFeedback.documentUrl,
      feedbackMessage = userFeedback.feedbackMessage,
      userEmail = userEmail,
      matchContext = userFeedback.matchContext
    )
}