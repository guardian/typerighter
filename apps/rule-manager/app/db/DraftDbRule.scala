package db

import db.DbRuleHelpers._
import model.{CreateRuleForm, UpdateRuleForm}
import play.api.libs.json.{Format, Json}
import play.api.mvc.Result
import play.api.mvc.Results.{InternalServerError, NotFound}
import scalikejdbc._

import java.time.ZonedDateTime
import scala.util.{Failure, Success, Try}

case class DraftDbRule(
    id: Option[Int],
    ruleType: String,
    pattern: Option[String] = None,
    replacement: Option[String] = None,
    category: Option[String] = None,
    tags: Option[String] = None,
    description: Option[String] = None,
    ignore: Boolean,
    notes: Option[String] = None,
    googleSheetId: Option[String] = None,
    forceRedRule: Option[Boolean] = None,
    advisoryRule: Option[Boolean] = None,
    createdAt: ZonedDateTime,
    createdBy: String,
    updatedAt: ZonedDateTime,
    updatedBy: String,
    revisionId: Int = 0
) extends DbRule {

  def toLive(reason: String): LiveDbRule = {
    LiveDbRule(
      id = this.id,
      ruleType = this.ruleType,
      pattern = this.pattern,
      replacement = this.replacement,
      category = this.category,
      tags = this.tags,
      description = this.description,
      notes = this.notes,
      googleSheetId = this.googleSheetId,
      forceRedRule = this.forceRedRule,
      advisoryRule = this.advisoryRule,
      revisionId = this.revisionId,
      createdAt = this.createdAt,
      createdBy = this.createdBy,
      updatedAt = this.updatedAt,
      updatedBy = this.updatedBy,
      reason = reason
    )
  }
}

object DraftDbRule extends SQLSyntaxSupport[DraftDbRule] {
  implicit val format: Format[DraftDbRule] = Json.format[DraftDbRule]

  override val tableName = "rules_draft"

  override val columns: Seq[String] = dbColumns ++ Seq(
    "ignore"
  )

  def fromResultName(r: ResultName[DraftDbRule])(rs: WrappedResultSet): DraftDbRule =
    autoConstruct(rs, r)

  def withUser(
      id: Option[Int],
      ruleType: String,
      pattern: Option[String] = None,
      replacement: Option[String] = None,
      category: Option[String] = None,
      tags: Option[String] = None,
      description: Option[String] = None,
      ignore: Boolean,
      notes: Option[String] = None,
      googleSheetId: Option[String] = None,
      forceRedRule: Option[Boolean] = None,
      advisoryRule: Option[Boolean] = None,
      user: String
  ) = {
    val createdAt = ZonedDateTime.now()
    DraftDbRule(
      id,
      ruleType,
      pattern,
      replacement,
      category,
      tags,
      description,
      ignore,
      notes,
      googleSheetId,
      forceRedRule,
      advisoryRule,
      createdAt,
      createdBy = user,
      updatedAt = createdAt,
      updatedBy = user
    )
  }

  val r = DraftDbRule.syntax("r")

  override val autoSession = AutoSession

  def find(id: Int)(implicit session: DBSession = autoSession): Option[DraftDbRule] = {
    withSQL {
      select.from(DraftDbRule as r).where.eq(r.id, id)
    }.map(DraftDbRule.fromResultName(r.resultName)).single().apply()
  }

  def findAll()(implicit session: DBSession = autoSession): List[DraftDbRule] = {
    withSQL(select.from(DraftDbRule as r).orderBy(r.id))
      .map(DraftDbRule.fromResultName(r.resultName))
      .list()
      .apply()
  }

  def countAll()(implicit session: DBSession = autoSession): Long = {
    withSQL(select(sqls.count).from(DraftDbRule as r)).map(rs => rs.long(1)).single().apply().get
  }

  def findBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Option[DraftDbRule] = {
    withSQL {
      select.from(DraftDbRule as r).where.append(where)
    }.map(DraftDbRule.fromResultName(r.resultName)).single().apply()
  }

  def findAllBy(where: SQLSyntax)(implicit session: DBSession = autoSession): List[DraftDbRule] = {
    withSQL {
      select.from(DraftDbRule as r).where.append(where)
    }.map(DraftDbRule.fromResultName(r.resultName)).list().apply()
  }

  def countBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Long = {
    withSQL {
      select(sqls.count).from(DraftDbRule as r).where.append(where)
    }.map(_.long(1)).single().apply().get
  }

  def create(
      ruleType: String,
      pattern: Option[String] = None,
      replacement: Option[String] = None,
      category: Option[String] = None,
      tags: Option[String] = None,
      description: Option[String] = None,
      ignore: Boolean,
      notes: Option[String] = None,
      googleSheetId: Option[String] = None,
      forceRedRule: Option[Boolean] = None,
      advisoryRule: Option[Boolean] = None,
      user: String
  )(implicit session: DBSession = autoSession): Try[DraftDbRule] = {
    val generatedKey = withSQL {
      insert
        .into(DraftDbRule)
        .namedValues(
          column.ruleType -> ruleType,
          column.pattern -> pattern,
          column.replacement -> replacement,
          column.category -> category,
          column.tags -> tags,
          column.description -> description,
          column.ignore -> ignore,
          column.notes -> notes,
          column.googleSheetId -> googleSheetId,
          column.forceRedRule -> forceRedRule,
          column.advisoryRule -> advisoryRule,
          column.createdBy -> user,
          column.updatedBy -> user
        )
    }.updateAndReturnGeneratedKey().apply()

    find(generatedKey.toInt) match {
      case Some(rule) => Success(rule)
      case None =>
        Failure(
          new Exception(
            s"Attempted to create a rule with id $generatedKey, but no result found attempting to read it back"
          )
        )
    }
  }

  def createFromFormRule(formRule: CreateRuleForm, user: String)(implicit
      session: DBSession = autoSession
  ) = {
    DraftDbRule.create(
      formRule.ruleType,
      formRule.pattern,
      formRule.replacement,
      formRule.category,
      formRule.tags,
      formRule.description,
      formRule.ignore,
      formRule.notes,
      formRule.googleSheetId,
      formRule.forceRedRule,
      formRule.advisoryRule,
      user
    )
  }

  def updateFromFormRule(
      formRule: UpdateRuleForm,
      id: Int,
      user: String
  )(implicit session: DBSession = autoSession): Either[Result, DraftDbRule] = {
    val updatedRule = DraftDbRule
      .find(id)
      .toRight(NotFound("Rule not found matching ID"))
      .map(existingRule =>
        existingRule.copy(
          id = Some(id),
          ruleType = formRule.ruleType.getOrElse(existingRule.ruleType),
          pattern = formRule.pattern.orElse(existingRule.pattern),
          replacement = formRule.replacement.orElse(existingRule.replacement),
          category = formRule.category.orElse(existingRule.category),
          tags = formRule.tags.orElse(existingRule.tags),
          description = formRule.description.orElse(existingRule.description),
          ignore = formRule.ignore.getOrElse(existingRule.ignore),
          notes = formRule.notes.orElse(existingRule.notes),
          googleSheetId = formRule.googleSheetId.orElse(existingRule.googleSheetId),
          forceRedRule = formRule.forceRedRule.orElse(existingRule.forceRedRule),
          advisoryRule = formRule.advisoryRule.orElse(existingRule.advisoryRule)
        )
      )
    updatedRule match {
      case Right(dbRule) => {
        DraftDbRule.save(dbRule, user).toEither match {
          case Left(e: Throwable) => Left(InternalServerError(e.getMessage()))
          case Right(dbRule)      => Right(dbRule)
        }
      }
      case Left(result) => Left(result)
    }
  }

  def batchInsert(
      entities: collection.Seq[DraftDbRule]
  )(implicit session: DBSession = autoSession): List[Int] = {
    val params: collection.Seq[Seq[(Symbol, Any)]] = entities.map(entity =>
      Seq(
        Symbol("ruleType") -> entity.ruleType,
        Symbol("pattern") -> entity.pattern,
        Symbol("replacement") -> entity.replacement,
        Symbol("category") -> entity.category,
        Symbol("tags") -> entity.tags,
        Symbol("description") -> entity.description,
        Symbol("ignore") -> entity.ignore,
        Symbol("notes") -> entity.notes,
        Symbol("googleSheetId") -> entity.googleSheetId,
        Symbol("forceRedRule") -> entity.forceRedRule,
        Symbol("advisoryRule") -> entity.advisoryRule,
        Symbol("createdBy") -> entity.createdBy,
        Symbol("createdAt") -> entity.createdAt,
        Symbol("updatedBy") -> entity.updatedBy,
        Symbol("updatedAt") -> entity.updatedAt
      )
    )
    SQL(s"""insert into $tableName(
      rule_type,
      pattern,
      replacement,
      category,
      tags,
      description,
      ignore,
      notes,
      google_sheet_id,
      force_red_rule,
      advisory_rule,
      created_by,
      created_at,
      updated_by,
      updated_at
    ) values (
      {ruleType},
      {pattern},
      {replacement},
      {category},
      {tags},
      {description},
      {ignore},
      {notes},
      {googleSheetId},
      {forceRedRule},
      {advisoryRule},
      {createdBy},
      {createdAt},
      {updatedBy},
      {updatedAt}
    )""").batchByName(params.toSeq: _*).apply[List]()
  }

  def save(entity: DraftDbRule, user: String)(implicit
      session: DBSession = autoSession
  ): Try[DraftDbRule] = {
    withSQL {
      update(DraftDbRule)
        .set(
          column.id -> entity.id,
          column.ruleType -> entity.ruleType,
          column.pattern -> entity.pattern,
          column.replacement -> entity.replacement,
          column.category -> entity.category,
          column.tags -> entity.tags,
          column.description -> entity.description,
          column.ignore -> entity.ignore,
          column.notes -> entity.notes,
          column.googleSheetId -> entity.googleSheetId,
          column.forceRedRule -> entity.forceRedRule,
          column.advisoryRule -> entity.advisoryRule,
          column.createdAt -> entity.createdAt,
          column.createdBy -> entity.createdBy,
          column.updatedAt -> ZonedDateTime.now(),
          column.updatedBy -> user,
          column.revisionId -> sqls"${column.revisionId} + 1"
        )
        .where
        .eq(column.id, entity.id)
    }.update().apply()

    find(entity.id.get)
      .toRight(
        new Exception(s"Error updating rule with id ${entity.id}: could not read updated rule")
      )
      .toTry
  }

  def destroy(entity: DraftDbRule)(implicit session: DBSession = autoSession): Int = {
    withSQL {
      delete.from(DraftDbRule).where.eq(column.id, entity.id)
    }.update().apply()
  }

  def destroyAll()(implicit session: DBSession = autoSession): Int = {
    withSQL {
      delete.from(DraftDbRule)
    }.update().apply()
  }
}
