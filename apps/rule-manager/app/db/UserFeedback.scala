package db

import play.api.libs.json.{Format, Json}
import scalikejdbc._

import java.time.OffsetDateTime
import scala.util.{Failure, Success, Try}

/** Feedback submitted to the user feedback API, with added user data from authentication.
  */
case class UserFeedback(
    id: Option[Int],
    app: String,
    stage: String,
    documentUrl: String,
    feedbackMessage: String,
    userEmail: String,
    matchId: Option[String],
    ruleId: Option[String],
    documentId: Option[String],
    matcherType: Option[String],
    suggestion: Option[String],
    matchIsMarkedAsCorrect: Option[Boolean],
    matchIsAdvisory: Option[Boolean],
    matchHasReplacement: Option[Boolean],
    matchedText: Option[String],
    matchContext: Option[String],
    createdAt: OffsetDateTime
)

object UserFeedback extends SQLSyntaxSupport[UserFeedback] {
  implicit val formats: Format[UserFeedback] = Json.format[UserFeedback]

  override val tableName = "user_feedback"

  override val columns = Seq(
    "id",
    "app",
    "stage",
    "document_url",
    "feedback_message",
    "user_email",
    "match_id",
    "rule_id",
    "document_id",
    "matcher_type",
    "suggestion",
    "match_is_marked_as_correct",
    "match_is_advisory",
    "match_has_replacement",
    "matched_text",
    "match_context",
    "created_at"
  )

  val uf = UserFeedback.syntax("uf")

  def fromRow(rs: WrappedResultSet): UserFeedback = {
    UserFeedback(
      id = rs.intOpt("id"),
      app = rs.string("app"),
      stage = rs.string("stage"),
      documentUrl = rs.string("document_url"),
      feedbackMessage = rs.string("feedback_message"),
      userEmail = rs.string("user_email"),
      matchId = rs.stringOpt("match_id"),
      ruleId = rs.stringOpt("rule_id"),
      documentId = rs.stringOpt("document_id"),
      matcherType = rs.stringOpt("matcher_type"),
      suggestion = rs.stringOpt("suggestion"),
      matchIsMarkedAsCorrect = rs.booleanOpt("match_is_marked_as_correct"),
      matchIsAdvisory = rs.booleanOpt("match_is_advisory"),
      matchHasReplacement = rs.booleanOpt("match_has_replacement"),
      matchedText = rs.stringOpt("matched_text"),
      matchContext = rs.stringOpt("match_context"),
      createdAt = rs.offsetDateTime("created_at")
    )
  }

  def find(id: Int)(implicit session: DBSession = autoSession): Option[UserFeedback] = {
    withSQL {
      select.from(UserFeedback as uf).where.eq(uf.id, id)
    }.map(fromRow).single().apply()
  }

  def findAll()(implicit session: DBSession = autoSession): List[UserFeedback] = {
    withSQL {
      select.from(UserFeedback as uf).orderBy(uf.createdAt).desc
    }.map(fromRow).list().apply()
  }

  def create(
      app: String,
      stage: String,
      documentUrl: String,
      feedbackMessage: String,
      userEmail: String,
      matchId: Option[String] = None,
      ruleId: Option[String] = None,
      documentId: Option[String] = None,
      matcherType: Option[String] = None,
      suggestion: Option[String] = None,
      matchIsMarkedAsCorrect: Option[Boolean] = None,
      matchIsAdvisory: Option[Boolean] = None,
      matchHasReplacement: Option[Boolean] = None,
      matchedText: Option[String] = None,
      matchContext: Option[String] = None
  )(implicit session: DBSession = autoSession): Try[UserFeedback] = {
    val generatedKey = withSQL {
      insert
        .into(UserFeedback)
        .namedValues(
          column.app -> app,
          column.stage -> stage,
          column.documentUrl -> documentUrl,
          column.feedbackMessage -> feedbackMessage,
          column.userEmail -> userEmail,
          column.matchId -> matchId,
          column.ruleId -> ruleId,
          column.documentId -> documentId,
          column.matcherType -> matcherType,
          column.suggestion -> suggestion,
          column.matchIsMarkedAsCorrect -> matchIsMarkedAsCorrect,
          column.matchIsAdvisory -> matchIsAdvisory,
          column.matchHasReplacement -> matchHasReplacement,
          column.matchedText -> matchedText,
          column.matchContext -> matchContext
        )
    }.updateAndReturnGeneratedKey().apply()
    find(generatedKey.toInt) match {
      case Some(feedback) => Success(feedback)
      case None =>
        Failure(
          new Exception(
            s"Attempted to create user feedback with id $generatedKey, but no result found attempting to read it back"
          )
        )
    }
  }
}
