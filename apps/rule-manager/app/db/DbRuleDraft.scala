package db

import db.DbRule._
import model.{CreateRuleForm, UpdateRuleForm}
import play.api.libs.json.{Format, Json}
import play.api.mvc.Result
import play.api.mvc.Results.{InternalServerError, NotFound}
import scalikejdbc._

import java.time.OffsetDateTime
import scala.util.{Failure, Success, Try}

case class DbRuleDraft(
    id: Option[Int],
    ruleType: String,
    pattern: Option[String] = None,
    replacement: Option[String] = None,
    category: Option[String] = None,
    tags: List[Int] = List.empty,
    description: Option[String] = None,
    ignore: Boolean,
    notes: Option[String] = None,
    externalId: Option[String] = None,
    forceRedRule: Option[Boolean] = None,
    advisoryRule: Option[Boolean] = None,
    createdAt: OffsetDateTime,
    createdBy: String,
    updatedAt: OffsetDateTime,
    updatedBy: String,
    revisionId: Int = 0,
    isPublished: Boolean,
    isArchived: Boolean
) extends DbRuleCommon {

  def toLive(reason: String): DbRuleLive = {
    id match {
      case None =>
        throw new Exception(
          s"Attempted to make live rule for rule with externalId $externalId, but the rule did not have an id"
        )
      case Some(id) =>
        DbRuleLive(
          ruleType = ruleType,
          pattern = pattern,
          replacement = replacement,
          category = category,
          tags = tags,
          description = description,
          notes = notes,
          externalId = externalId,
          forceRedRule = forceRedRule,
          advisoryRule = advisoryRule,
          revisionId = revisionId,
          createdAt = createdAt,
          createdBy = createdBy,
          updatedAt = updatedAt,
          updatedBy = updatedBy,
          reason = reason,
          ruleOrder = id
        )
    }
  }
}

object DbRuleDraft extends SQLSyntaxSupport[DbRuleDraft] {
  implicit val format: Format[DbRuleDraft] = Json.format[DbRuleDraft]

  override val tableName = "rules_draft"

  override val columns: Seq[String] = dbColumns ++ Seq(
    "ignore",
    "id",
    "is_archived"
  )

  def fromRow(rs: WrappedResultSet): DbRuleDraft = {
    DbRuleDraft(
      id = rs.intOpt("id"),
      ruleType = rs.string("rule_type"),
      pattern = rs.stringOpt("pattern"),
      replacement = rs.stringOpt("replacement"),
      category = rs.stringOpt("category"),
      tags = rs.array("tags").getArray.asInstanceOf[Array[Integer]].toList.map { _.intValue() },
      description = rs.stringOpt("description"),
      ignore = rs.boolean("ignore"),
      notes = rs.stringOpt("notes"),
      externalId = rs.stringOpt("external_id"),
      forceRedRule = rs.booleanOpt("force_red_rule"),
      advisoryRule = rs.booleanOpt("advisory_rule"),
      createdAt = rs.offsetDateTime("created_at"),
      createdBy = rs.string("created_by"),
      updatedAt = rs.offsetDateTime("updated_at"),
      updatedBy = rs.string("updated_by"),
      revisionId = rs.int("revision_id"),
      isPublished = rs.boolean("is_published"),
      isArchived = rs.boolean("is_archived")
    )
  }

  def withUser(
      id: Option[Int],
      ruleType: String,
      pattern: Option[String] = None,
      replacement: Option[String] = None,
      category: Option[String] = None,
      tags: List[Int] = List.empty,
      description: Option[String] = None,
      ignore: Boolean,
      notes: Option[String] = None,
      externalId: Option[String] = None,
      forceRedRule: Option[Boolean] = None,
      advisoryRule: Option[Boolean] = None,
      user: String
  ) = {
    val createdAt = OffsetDateTime.now()
    DbRuleDraft(
      id,
      ruleType,
      pattern,
      replacement,
      category,
      tags,
      description,
      ignore,
      notes,
      externalId,
      forceRedRule,
      advisoryRule,
      createdAt,
      createdBy = user,
      updatedAt = createdAt,
      updatedBy = user,
      isPublished = false,
      isArchived = false
    )
  }

  val rd = DbRuleDraft.syntax("rd")
  val rl = DbRuleLive.syntax("rl")
  val rt = RuleTagDraft.syntax("rt")

  val tagColumn =
    sqls"COALESCE(ARRAY_AGG(${rt.tagId}) FILTER (WHERE ${rt.tagId} IS NOT NULL), '{}') AS tags"

  val isPublishedColumn = sqls"${rl.externalId} IS NOT NULL AS is_published"

  val dbColumnsToFind = SQLSyntax.createUnsafely(
    rd.columns.filter(_.value != "tags").map(c => s"${rd.tableAliasName}.${c.value}").mkString(", ")
  )

  override val autoSession = AutoSession

  def find(id: Int)(implicit session: DBSession = autoSession): Option[DbRuleDraft] = {
    withSQL {
      select(dbColumnsToFind, isPublishedColumn, tagColumn)
        .from(DbRuleDraft as rd)
        .leftJoin(DbRuleLive as rl)
        .on(sqls"${rd.externalId} = ${rl.externalId} and ${rl.isActive} = true")
        .leftJoin(RuleTagDraft as rt)
        .on(rd.id, rt.ruleId)
        .where
        .eq(rd.id, id)
        .groupBy(dbColumnsToFind, rl.externalId)
    }.map(DbRuleDraft.fromRow)
      .single()
      .apply()
  }

  def findRules(ids: List[Int])(implicit session: DBSession = autoSession): List[DbRuleDraft] = {
    withSQL {
      select(dbColumnsToFind, isPublishedColumn, tagColumn)
        .from(DbRuleDraft as rd)
        .leftJoin(DbRuleLive as rl)
        .on(sqls"${rd.externalId} = ${rl.externalId} and ${rl.isActive} = true")
        .leftJoin(RuleTagDraft as rt)
        .on(rd.id, rt.ruleId)
        .where
        .in(rd.id, ids)
        .groupBy(dbColumnsToFind, rl.externalId)
    }
      .map(DbRuleDraft.fromRow)
      .list()
      .apply()
  }

  def findAll()(implicit session: DBSession = autoSession): List[DbRuleDraft] = {
    withSQL {
      select(dbColumnsToFind, isPublishedColumn, tagColumn)
        .from(DbRuleDraft as rd)
        .leftJoin(DbRuleLive as rl)
        .on(sqls"${rd.externalId} = ${rl.externalId} and ${rl.isActive} = true")
        .leftJoin(RuleTagDraft as rt)
        .on(rd.id, rt.ruleId)
        .groupBy(dbColumnsToFind, rl.externalId)
        .orderBy(rd.id)
    }.map(DbRuleDraft.fromRow)
      .list()
      .apply()
  }

  def countAll()(implicit session: DBSession = autoSession): Long = {
    withSQL(select(sqls.count).from(DbRuleDraft as rd)).map(rs => rs.long(1)).single().apply().get
  }

  def countBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Long = {
    withSQL {
      select(sqls.count).from(DbRuleDraft as rd).where.append(where)
    }.map(_.long(1)).single().apply().get
  }

  def create(
      ruleType: String,
      pattern: Option[String] = None,
      replacement: Option[String] = None,
      category: Option[String] = None,
      description: Option[String] = None,
      ignore: Boolean,
      notes: Option[String] = None,
      forceRedRule: Option[Boolean] = None,
      advisoryRule: Option[Boolean] = None,
      user: String,
      isArchived: Boolean = false,
      tags: List[Int] = List.empty
  )(implicit session: DBSession = autoSession): Try[DbRuleDraft] = {
    val id = withSQL {
      insert
        .into(DbRuleDraft)
        .namedValues(
          column.ruleType -> ruleType,
          column.pattern -> pattern,
          column.replacement -> replacement,
          column.category -> category,
          column.description -> description,
          column.ignore -> ignore,
          column.notes -> notes,
          column.forceRedRule -> forceRedRule,
          column.advisoryRule -> advisoryRule,
          column.createdBy -> user,
          column.updatedBy -> user,
          column.isArchived -> isArchived
        )
    }.updateAndReturnGeneratedKey().apply().toInt

    val tagRelations = tags.map(tagId => RuleTagDraft(id, tagId))
    RuleTagDraft.batchInsert(tagRelations)

    find(id) match {
      case Some(rule) => Success(rule)
      case None =>
        Failure(
          new Exception(
            s"Attempted to create a rule with id $id, but no result found attempting to read it back"
          )
        )
    }
  }

  def createFromFormRule(formRule: CreateRuleForm, user: String)(implicit
      session: DBSession = autoSession
  ) = {
    DbRuleDraft.create(
      ruleType = formRule.ruleType,
      pattern = formRule.pattern,
      replacement = formRule.replacement,
      category = formRule.category,
      tags = formRule.tags.getOrElse(List.empty),
      description = formRule.description,
      ignore = formRule.ignore,
      notes = formRule.notes,
      forceRedRule = formRule.forceRedRule,
      advisoryRule = formRule.advisoryRule,
      user = user
    )
  }

  def updateFromFormRule(
      formRule: UpdateRuleForm,
      id: Int,
      user: String
  )(implicit session: DBSession = autoSession): Either[Result, DbRuleDraft] = {
    val updatedRule = DbRuleDraft
      .find(id)
      .toRight(NotFound("Rule not found matching ID"))
      .map(existingRule =>
        existingRule.copy(
          ruleType = formRule.ruleType.getOrElse(existingRule.ruleType),
          pattern = formRule.pattern,
          replacement = formRule.replacement,
          category = formRule.category,
          tags = formRule.tags.getOrElse(existingRule.tags),
          description = formRule.description,
          advisoryRule = formRule.advisoryRule
        )
      )
    updatedRule match {
      case Right(dbRule) => {
        DbRuleDraft.save(dbRule, user).toEither match {
          case Left(e: Throwable) => Left(InternalServerError(e.getMessage))
          case Right(dbRule)      => Right(dbRule)
        }
      }
      case Left(result) => Left(result)
    }
  }
  def batchUpdate(ids: List[Int], category: String, tags: List[Int], user: String)(implicit
      session: DBSession = autoSession
  ): Try[List[DbRuleDraft]] = {
    Try {
      withSQL {
        update(DbRuleDraft)
          .set(
            column.category -> category,
            column.updatedBy -> user,
            column.revisionId -> sqls"${column.revisionId} + 1"
          )
          .where
          .in(column.id, ids)
      }.update().apply()

      val newTag = ids.flatMap(ruleId => tags.map(tagId => RuleTagDraft(ruleId, tagId)))
      RuleTagDraft.batchInsert(newTag)

      val rules = findRules(ids)

      rules
    }
  }

  def batchInsert(
      entities: collection.Seq[DbRuleDraft]
  )(implicit session: DBSession = autoSession): List[Int] = {
    val params: collection.Seq[Seq[(Symbol, Any)]] = entities.map(entity =>
      Seq(
        Symbol("ruleType") -> entity.ruleType,
        Symbol("pattern") -> entity.pattern,
        Symbol("replacement") -> entity.replacement,
        Symbol("category") -> entity.category,
        Symbol("description") -> entity.description,
        Symbol("ignore") -> entity.ignore,
        Symbol("notes") -> entity.notes,
        Symbol("externalId") -> entity.externalId,
        Symbol("forceRedRule") -> entity.forceRedRule,
        Symbol("advisoryRule") -> entity.advisoryRule,
        Symbol("createdBy") -> entity.createdBy,
        Symbol("createdAt") -> entity.createdAt,
        Symbol("updatedBy") -> entity.updatedBy,
        Symbol("updatedAt") -> entity.updatedAt,
        Symbol("isArchived") -> entity.isArchived
      )
    )
    SQL(s"""insert into $tableName(
      rule_type,
      pattern,
      replacement,
      category,
      description,
      ignore,
      notes,
      external_id,
      force_red_rule,
      advisory_rule,
      created_by,
      created_at,
      updated_by,
      updated_at,
      is_archived
    ) values (
      {ruleType},
      {pattern},
      {replacement},
      {category},
      {description},
      {ignore},
      {notes},
      {externalId},
      {forceRedRule},
      {advisoryRule},
      {createdBy},
      {createdAt},
      {updatedBy},
      {updatedAt},
      {isArchived}
    )""").batchByName(params.toSeq: _*).apply[Seq]().toList

    // Get the last inserted ID to produce the ids generated in the batch update.
    val lastInsertedId = sql"SELECT currval(pg_get_serial_sequence($tableName,'id')) as last_id"
      .map(_.int("last_id"))
      .single()
      .apply()
      .get

    val firstInsertedId = (lastInsertedId - entities.size + 1)
    val ruleIds = (firstInsertedId to lastInsertedId).toList
    val ruleTags = ruleIds.zip(entities).flatMap { case (id, rule) =>
      rule.tags.map(tag => RuleTagDraft(id, tag))
    }

    RuleTagDraft.batchInsert(ruleTags)

    ruleIds
  }

  def save(entity: DbRuleDraft, user: String)(implicit
      session: DBSession = autoSession
  ): Try[DbRuleDraft] = {
    val id = withSQL {
      update(DbRuleDraft)
        .set(
          column.ruleType -> entity.ruleType,
          column.pattern -> entity.pattern,
          column.replacement -> entity.replacement,
          column.category -> entity.category,
          column.description -> entity.description,
          column.ignore -> entity.ignore,
          column.notes -> entity.notes,
          column.externalId -> entity.externalId,
          column.forceRedRule -> entity.forceRedRule,
          column.advisoryRule -> entity.advisoryRule,
          column.createdAt -> entity.createdAt,
          column.createdBy -> entity.createdBy,
          column.updatedAt -> OffsetDateTime.now(),
          column.updatedBy -> user,
          column.isArchived -> entity.isArchived,
          column.revisionId -> sqls"${column.revisionId} + 1"
        )
        .where
        .eq(column.id, entity.id)
    }.updateAndReturnGeneratedKey().apply().toInt

    val tagRelations = entity.tags.map(tagId => RuleTagDraft(id, tagId))
    RuleTagDraft.batchInsert(tagRelations)

    find(entity.id.get)
      .toRight(
        new Exception(s"Error updating rule with id ${entity.id}: could not read updated rule")
      )
      .toTry
  }

  def destroy(entity: DbRuleDraft)(implicit session: DBSession = autoSession): Int = {
    withSQL {
      delete.from(DbRuleDraft).where.eq(column.id, entity.id)
    }.update().apply()
  }

  def destroyAll()(implicit session: DBSession = autoSession): Int = {
    withSQL {
      delete.from(DbRuleDraft)
    }.update().apply()
  }
}
