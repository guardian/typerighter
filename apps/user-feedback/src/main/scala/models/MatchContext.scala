package models

import play.api.libs.json.{Format, Json}

/**
 * When feedback is given in the context of a match, we capture additional
 * information about that context here.
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