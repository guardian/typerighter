package db

import models.UserFeedbackWithAuthentication
import play.api.libs.json.{Format, Json}
import scalikejdbc._

import java.time.OffsetDateTime
import scala.util.{Failure, Success, Try}
import play.api.Logging

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
    externalRuleId: Option[String],
    ruleId: Option[Int],
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

object UserFeedback extends SQLSyntaxSupport[UserFeedback] with Logging {
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
    "external_rule_id",
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
    val u = uf.resultName
    UserFeedback(
      id = rs.intOpt(u.id),
      app = rs.string(u.app),
      stage = rs.string(u.stage),
      documentUrl = rs.string(u.documentUrl),
      feedbackMessage = rs.string(u.feedbackMessage),
      userEmail = rs.string(u.userEmail),
      matchId = rs.stringOpt(u.matchId),
      externalRuleId = rs.stringOpt(u.externalRuleId),
      ruleId = rs.intOpt(u.ruleId),
      documentId = rs.stringOpt(u.documentId),
      matcherType = rs.stringOpt(u.matcherType),
      suggestion = rs.stringOpt(u.suggestion),
      matchIsMarkedAsCorrect = rs.booleanOpt(u.matchIsMarkedAsCorrect),
      matchIsAdvisory = rs.booleanOpt(u.matchIsAdvisory),
      matchHasReplacement = rs.booleanOpt(u.matchHasReplacement),
      matchedText = rs.stringOpt(u.matchedText),
      matchContext = rs.stringOpt(u.matchContext),
      createdAt = rs.offsetDateTime(u.createdAt)
    )
  }

  def fromUserFeedbackWithAuthentication(
      feedback: UserFeedbackWithAuthentication,
      createdAt: OffsetDateTime = OffsetDateTime.now()
  ): UserFeedback = {
    UserFeedback(
      id = None,
      app = feedback.app,
      stage = feedback.stage,
      documentUrl = feedback.documentUrl,
      feedbackMessage = feedback.feedbackMessage,
      userEmail = feedback.userEmail,
      matchId = feedback.matchContext.map(_.matchId),
      externalRuleId = feedback.matchContext.map(_.ruleId),
      ruleId = None,
      documentId = feedback.matchContext.map(_.documentId),
      matcherType = feedback.matchContext.map(_.matcherType),
      suggestion = feedback.matchContext.flatMap(_.suggestion),
      matchIsMarkedAsCorrect = feedback.matchContext.map(_.matchIsMarkedAsCorrect),
      matchIsAdvisory = feedback.matchContext.map(_.matchIsAdvisory),
      matchHasReplacement = feedback.matchContext.map(_.matchHasReplacement),
      matchedText = feedback.matchContext.map(_.matchedText),
      matchContext = feedback.matchContext.map(_.matchContext),
      createdAt = createdAt
    )
  }

  def find(id: Int)(implicit session: DBSession = autoSession): Option[UserFeedback] = {
    withSQL {
      select.from(UserFeedback as uf).where.eq(uf.id, id)
    }.map(fromRow).single().apply()
  }

  def findAll()(implicit session: DBSession = autoSession): List[UserFeedback] = {
    withSQL {
      select.from(UserFeedback as uf).orderBy(uf.createdAt, uf.id).desc
    }.map(fromRow).list().apply()
  }

  def create(
      userFeedback: UserFeedbackWithAuthentication
  )(implicit session: DBSession = autoSession): Try[UserFeedback] = {
    val maybeExternalRuleId = userFeedback.matchContext.map(_.ruleId)
    val ruleId = for {
      externalId <- maybeExternalRuleId
      rule <- DbRuleDraft.findByExternalId(externalId).orElse {
        logger.warn(
          s"Feedback received for external id $externalId, but no rule was found with that id"
        )
        None
      }
      ruleId <- rule.id
    } yield ruleId

    val generatedKey = withSQL {
      insert
        .into(UserFeedback)
        .namedValues(
          column.app -> userFeedback.app,
          column.stage -> userFeedback.stage,
          column.documentUrl -> userFeedback.documentUrl,
          column.feedbackMessage -> userFeedback.feedbackMessage,
          column.userEmail -> userFeedback.userEmail,
          column.matchId -> userFeedback.matchContext.map(_.matchId),
          column.externalRuleId -> maybeExternalRuleId,
          column.ruleId -> ruleId,
          column.documentId -> userFeedback.matchContext.map(_.documentId),
          column.matcherType -> userFeedback.matchContext.map(_.matcherType),
          column.suggestion -> userFeedback.matchContext.map(_.suggestion),
          column.matchIsMarkedAsCorrect -> userFeedback.matchContext.map(_.matchIsMarkedAsCorrect),
          column.matchIsAdvisory -> userFeedback.matchContext.map(_.matchIsAdvisory),
          column.matchHasReplacement -> userFeedback.matchContext.map(_.matchHasReplacement),
          column.matchedText -> userFeedback.matchContext.map(_.matchedText),
          column.matchContext -> userFeedback.matchContext.map(_.matchContext)
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
