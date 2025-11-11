package db

import db.DbRule._
import play.api.libs.json.{Format, Json}
import scalikejdbc._

import java.time.OffsetDateTime
import scala.util.Try
import java.time.temporal.ChronoUnit

trait DbRuleLiveFields {
  def reason: String
}

case class DbRuleLive(
    ruleType: String,
    pattern: Option[String] = None,
    replacement: Option[String] = None,
    category: Option[String] = None,
    tags: List[Int] = List.empty,
    description: Option[String] = None,
    notes: Option[String] = None,
    externalId: Option[String] = None,
    forceRedRule: Option[Boolean] = None,
    advisoryRule: Option[Boolean] = None,
    createdAt: OffsetDateTime,
    createdBy: String,
    updatedAt: OffsetDateTime,
    updatedBy: String,
    revisionId: Int = 0,
    ruleOrder: Int = 0,
    isActive: Boolean = false,
    reason: String
) extends DbRuleCommon
    with DbRuleLiveFields

object DbRuleLive extends SQLSyntaxSupport[DbRuleLive] {
  implicit val format: Format[DbRuleLive] = Json.format[DbRuleLive]

  override val tableName = "rules_live"

  override val columns: Seq[String] = dbColumns ++ Seq(
    "reason",
    "is_active"
  )

  val rtl = RuleTagLive.syntax("rt")
  val r = DbRuleLive.syntax("r")

  override val autoSession = AutoSession

  def fromRow(rs: WrappedResultSet): DbRuleLive = {
    DbRuleLive(
      ruleType = rs.string("rule_type"),
      pattern = rs.stringOpt("pattern"),
      replacement = rs.stringOpt("replacement"),
      category = rs.stringOpt("category"),
      tags = rs.array("tags").getArray.asInstanceOf[Array[Integer]].toList.map { _.intValue() },
      description = rs.stringOpt("description"),
      notes = rs.stringOpt("notes"),
      externalId = rs.stringOpt("external_id"),
      forceRedRule = rs.booleanOpt("force_red_rule"),
      advisoryRule = rs.booleanOpt("advisory_rule"),
      createdAt = rs.offsetDateTime("created_at"),
      createdBy = rs.string("created_by"),
      updatedAt = rs.offsetDateTime("updated_at"),
      updatedBy = rs.string("updated_by"),
      revisionId = rs.int("revision_id"),
      reason = rs.string("reason"),
      isActive = rs.boolean("is_active"),
      ruleOrder = rs.int("rule_order")
    )
  }

  val dbColumnsToFind = SQLSyntax.createUnsafely(
    r.columns.filter(_.value != "tags").map(c => s"${r.tableAliasName}.${c.value}").mkString(", ")
  )

  val tagColumn =
    sqls"COALESCE(ARRAY_AGG(${rtl.tagId}) FILTER (WHERE ${rtl.tagId} IS NOT NULL), '{}') AS tags"

  def findRevision(externalId: String, revisionId: Int)(implicit
      session: DBSession = autoSession
  ): Option[DbRuleLive] = {
    withSQL {
      select(dbColumnsToFind, tagColumn)
        .from(DbRuleLive as r)
        .leftJoin(RuleTagLive as rtl)
        .on(
          sqls"${r.externalId} = ${rtl.ruleExternalId} and ${r.revisionId} = ${rtl.ruleRevisionId}"
        )
        .where
        .eq(r.externalId, externalId)
        .and
        .eq(r.revisionId, revisionId)
        .groupBy(dbColumnsToFind)
    }.map(DbRuleLive.fromRow).single().apply()
  }

  def findLatestRevision(externalId: String)(implicit
      session: DBSession = autoSession
  ): Option[DbRuleLive] = {
    withSQL {
      select(dbColumnsToFind, tagColumn)
        .from(DbRuleLive as r)
        .leftJoin(RuleTagLive as rtl)
        .on(
          sqls"${r.externalId} = ${rtl.ruleExternalId} and ${r.revisionId} = ${rtl.ruleRevisionId}"
        )
        .where
        .eq(r.externalId, externalId)
        .groupBy(dbColumnsToFind)
        .orderBy(r.revisionId.desc)
        .limit(1)
    }.map(DbRuleLive.fromRow).single().apply()
  }

  /** Find live rules by `externalId`. Because there may be many inactive live rules with the same
    * id, unlike the `find` method for draft rules, this method returns a list. To return a single
    * rule, use `findRevision` or `findLatestRevision`.
    */
  def find(
      externalId: String
  )(implicit session: DBSession = autoSession): List[DbRuleLive] = {
    withSQL {
      select(dbColumnsToFind, tagColumn)
        .from(DbRuleLive as r)
        .leftJoin(RuleTagLive as rtl)
        .on(
          sqls"${r.externalId} = ${rtl.ruleExternalId} and ${r.revisionId} = ${rtl.ruleRevisionId}"
        )
        .where
        .eq(r.externalId, externalId)
        .groupBy(dbColumnsToFind)
    }.map(DbRuleLive.fromRow).list().apply()
  }

  def findAll()(implicit session: DBSession = autoSession): List[DbRuleLive] = {
    withSQL(
      select(dbColumnsToFind, tagColumn)
        .from(DbRuleLive as r)
        .leftJoin(RuleTagLive as rtl)
        .on(
          sqls"${r.externalId} = ${rtl.ruleExternalId} and ${r.revisionId} = ${rtl.ruleRevisionId}"
        )
        .groupBy(dbColumnsToFind)
        .orderBy(r.ruleOrder)
    )
      .map(DbRuleLive.fromRow)
      .list()
      .apply()
  }

  def findAllActive()(implicit session: DBSession = autoSession): List[DbRuleLive] = {
    withSQL(
      select(dbColumnsToFind, tagColumn)
        .from(DbRuleLive as r)
        .leftJoin(RuleTagLive as rtl)
        .on(
          sqls"${r.externalId} = ${rtl.ruleExternalId} and ${r.revisionId} = ${rtl.ruleRevisionId}"
        )
        .where
        .eq(r.isActive, true)
        .groupBy(dbColumnsToFind)
        .orderBy(r.ruleOrder)
    )
      .fetchSize(1000)
      .map(DbRuleLive.fromRow)
      .list()
      .apply()
  }

  def findAllDictionaryRules()(implicit session: DBSession = autoSession): List[DbRuleLive] = {
    withSQL {
      select(dbColumnsToFind, tagColumn)
        .from(DbRuleLive as r)
        .leftJoin(RuleTagLive as rtl)
        .on(
          sqls"${r.externalId} = ${rtl.ruleExternalId} and ${r.revisionId} = ${rtl.ruleRevisionId}"
        )
        .where
        .eq(r.ruleType, "dictionary")
        .groupBy(dbColumnsToFind, r.externalId, r.revisionId)
        .orderBy(r.ruleOrder)
    }.map(DbRuleLive.fromRow)
      .list()
      .apply()
  }

  def setInactive(externalId: String, user: String)(implicit
      session: DBSession = autoSession
  ): Option[DbRuleLive] = {
    withSQL {
      update(DbRuleLive)
        .set(
          column.isActive -> false,
          column.updatedBy -> user,
          column.updatedAt -> OffsetDateTime.now.truncatedTo(ChronoUnit.MICROS)
        )
        .where
        .eq(column.externalId, externalId)
        .and
        .eq(column.isActive, true)
    }.update().apply()

    findLatestRevision(externalId)
  }

  /** Create a new live rule. This rule will supercede the previous active live rule.
    */
  def create(liveRule: DbRuleLive, user: String)(implicit
      session: DBSession = autoSession
  ): Try[DbRuleLive] = Try {
    withSQL {
      update(DbRuleLive)
        .set(column.isActive -> false)
        .where
        .eq(column.isActive, true)
        .and
        .eq(column.externalId, liveRule.externalId)
    }.update().apply()

    val generatedKey = withSQL {
      insert
        .into(DbRuleLive)
        .namedValues(
          column.ruleType -> liveRule.ruleType,
          column.pattern -> liveRule.pattern,
          column.replacement -> liveRule.replacement,
          column.category -> liveRule.category,
          column.description -> liveRule.description,
          column.notes -> liveRule.notes,
          column.externalId -> liveRule.externalId,
          column.forceRedRule -> liveRule.forceRedRule,
          column.advisoryRule -> liveRule.advisoryRule,
          column.reason -> liveRule.reason,
          column.revisionId -> liveRule.revisionId,
          column.createdBy -> user,
          column.updatedBy -> user,
          column.ruleOrder -> liveRule.ruleOrder,
          column.isActive -> true
        )
        .returning(column.externalId)
    }.map(_.string(column.externalId)).single().apply()

    val tagRelations =
      liveRule.tags.map(tagId => RuleTagLive(liveRule.externalId.get, liveRule.revisionId, tagId))
    RuleTagLive.batchInsert(tagRelations)

    findRevision(generatedKey.get, liveRule.revisionId) match {
      case Some(rule) => rule
      case None =>
        throw new Exception(
          s"Attempted to create a rule with id $generatedKey, but no result found attempting to read it back"
        )
    }
  }

  def batchInsert(
      entities: collection.Seq[DbRuleLive]
  )(implicit session: DBSession = autoSession): Unit = {
    val params: collection.Seq[Seq[(Symbol, Any)]] = entities.map(entity =>
      Seq(
        Symbol("ruleType") -> entity.ruleType,
        Symbol("pattern") -> entity.pattern,
        Symbol("replacement") -> entity.replacement,
        Symbol("category") -> entity.category,
        Symbol("description") -> entity.description,
        Symbol("notes") -> entity.notes,
        Symbol("externalId") -> entity.externalId,
        Symbol("forceRedRule") -> entity.forceRedRule,
        Symbol("advisoryRule") -> entity.advisoryRule,
        Symbol("createdBy") -> entity.createdBy,
        Symbol("createdAt") -> entity.createdAt,
        Symbol("updatedBy") -> entity.updatedBy,
        Symbol("updatedAt") -> entity.updatedAt,
        Symbol("reason") -> entity.reason,
        Symbol("isActive") -> entity.isActive,
        Symbol("ruleOrder") -> entity.ruleOrder
      )
    )
    SQL(s"""insert into $tableName(
      rule_type,
      pattern,
      replacement,
      category,
      description,
      notes,
      external_id,
      force_red_rule,
      advisory_rule,
      created_at,
      created_by,
      updated_at,
      updated_by,
      reason,
      is_active,
      rule_order
    ) values (
      {ruleType},
      {pattern},
      {replacement},
      {category},
      {description},
      {notes},
      {externalId},
      {forceRedRule},
      {advisoryRule},
      {createdAt},
      {createdBy},
      {updatedAt},
      {updatedBy},
      {reason},
      {isActive},
      {ruleOrder}
    )""").batchByName(params.toSeq: _*).apply[List]()
    val ruleTags = entities.flatMap(entity =>
      entity.tags.map(tag => RuleTagLive(entity.externalId.get, entity.revisionId, tag))
    )
    RuleTagLive.batchInsert(ruleTags)
    ()
  }

  def destroyDictionaryRules()(implicit session: DBSession = autoSession): Int = {
    withSQL {
      delete
        .from(DbRuleLive as r)
        .where
        .eq(r.ruleType, "dictionary")
    }.update().apply()
  }

  def destroyAll()(implicit session: DBSession = autoSession): Int = {
    withSQL {
      delete.from(DbRuleLive)
    }.update().apply()
  }
}
