package db

import model.{CreateRuleForm, UpdateRuleForm}
import play.api.libs.json.{Format, JsValue, Json}
import play.api.mvc.Result
import play.api.mvc.Results.{InternalServerError, NotFound}
import scalikejdbc._

import java.time.{LocalDateTime, ZonedDateTime}
import scala.util.{Failure, Success, Try}

case class DbRule(
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
)

object DbRule extends SQLSyntaxSupport[DbRule] {
  implicit val format: Format[DbRule] = Json.format[DbRule]

  override val tableName = "rules"

  override val columns = Seq(
    "id",
    "rule_type",
    "pattern",
    "replacement",
    "category",
    "tags",
    "description",
    "ignore",
    "notes",
    "google_sheet_id",
    "force_red_rule",
    "advisory_rule",
    "created_at",
    "created_by",
    "updated_at",
    "updated_by",
    "revision_id"
  )

  def fromResultName(r: ResultName[DbRule])(rs: WrappedResultSet): DbRule = autoConstruct(rs, r)

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
    DbRule(
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

  val r = DbRule.syntax("r")

  override val autoSession = AutoSession

  def find(id: Int)(implicit session: DBSession = autoSession): Option[DbRule] = {
    withSQL {
      select.from(DbRule as r).where.eq(r.id, id)
    }.map(DbRule.fromResultName(r.resultName)).single.apply()
  }

  def findAll()(implicit session: DBSession = autoSession): List[DbRule] = {
    withSQL(select.from(DbRule as r).orderBy(r.id))
      .map(DbRule.fromResultName(r.resultName))
      .list
      .apply()
  }

  def countAll()(implicit session: DBSession = autoSession): Long = {
    withSQL(select(sqls.count).from(DbRule as r)).map(rs => rs.long(1)).single.apply().get
  }

  def findBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Option[DbRule] = {
    withSQL {
      select.from(DbRule as r).where.append(where)
    }.map(DbRule.fromResultName(r.resultName)).single.apply()
  }

  def findAllBy(where: SQLSyntax)(implicit session: DBSession = autoSession): List[DbRule] = {
    withSQL {
      select.from(DbRule as r).where.append(where)
    }.map(DbRule.fromResultName(r.resultName)).list.apply()
  }

  def countBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Long = {
    withSQL {
      select(sqls.count).from(DbRule as r).where.append(where)
    }.map(_.long(1)).single.apply().get
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
  )(implicit session: DBSession = autoSession): Try[DbRule] = {
    val generatedKey = withSQL {
      insert
        .into(DbRule)
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
    }.updateAndReturnGeneratedKey.apply()

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
    DbRule.create(
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
  )(implicit session: DBSession = autoSession): Either[Result, DbRule] = {
    val updatedRule = DbRule
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
        DbRule.save(dbRule, user).toEither match {
          case Left(e: Throwable) => Left(InternalServerError(e.getMessage()))
          case Right(dbRule)      => Right(dbRule)
        }
      }
      case Left(result) => Left(result)
    }
  }

  def batchInsert(
      entities: collection.Seq[DbRule]
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
    SQL("""insert into rules(
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

  def save(entity: DbRule, user: String)(implicit session: DBSession = autoSession): Try[DbRule] = {
    withSQL {
      update(DbRule)
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
    }.update.apply()

    find(entity.id.get)
      .toRight(
        new Exception(s"Error updating rule with id ${entity.id}: could not read updated rule")
      )
      .toTry
  }

  def destroy(entity: DbRule)(implicit session: DBSession = autoSession): Int = {
    withSQL {
      delete.from(DbRule).where.eq(column.id, entity.id)
    }.update.apply()
  }

  def destroyAll()(implicit session: DBSession = autoSession): Int = {
    withSQL {
      delete.from(DbRule)
    }.update.apply()
  }
}
