package db

import db.DbRule._
import play.api.libs.json.{Format, Json}
import scalikejdbc._

import java.time.ZonedDateTime

trait DbRuleLiveFields {
  def reason: String
}

case class DbRuleLive(
    id: Option[Int],
    ruleType: String,
    pattern: Option[String] = None,
    replacement: Option[String] = None,
    category: Option[String] = None,
    tags: Option[String] = None,
    description: Option[String] = None,
    notes: Option[String] = None,
    googleSheetId: Option[String] = None,
    forceRedRule: Option[Boolean] = None,
    advisoryRule: Option[Boolean] = None,
    createdAt: ZonedDateTime,
    createdBy: String,
    updatedAt: ZonedDateTime,
    updatedBy: String,
    revisionId: Int = 0,
    reason: String
) extends DbRuleCommon
    with DbRuleLiveFields

object DbRuleLive extends SQLSyntaxSupport[DbRuleLive] {
  implicit val format: Format[DbRuleLive] = Json.format[DbRuleLive]

  override val tableName = "rules_live"

  override val columns: Seq[String] = dbColumns ++ Seq(
    "reason"
  )

  def fromResultName(r: ResultName[DbRuleLive])(rs: WrappedResultSet): DbRuleLive =
    autoConstruct(rs, r)

  val r = DbRuleLive.syntax("r")

  override val autoSession = AutoSession

  def find(id: Int)(implicit session: DBSession = autoSession): Option[DbRuleLive] = {
    withSQL {
      select.from(DbRuleLive as r).where.eq(r.id, id)
    }.map(DbRuleLive.fromResultName(r.resultName)).single().apply()
  }

  def findAll()(implicit session: DBSession = autoSession): List[DbRuleLive] = {
    withSQL(select.from(DbRuleLive as r).orderBy(r.id))
      .map(DbRuleLive.fromResultName(r.resultName))
      .list()
      .apply()
  }

  def batchInsert(
      entities: collection.Seq[DbRuleLive]
  )(implicit session: DBSession = autoSession): List[Int] = {
    val params: collection.Seq[Seq[(Symbol, Any)]] = entities.map(entity =>
      Seq(
        Symbol("ruleType") -> entity.ruleType,
        Symbol("pattern") -> entity.pattern,
        Symbol("replacement") -> entity.replacement,
        Symbol("category") -> entity.category,
        Symbol("tags") -> entity.tags,
        Symbol("description") -> entity.description,
        Symbol("notes") -> entity.notes,
        Symbol("googleSheetId") -> entity.googleSheetId,
        Symbol("forceRedRule") -> entity.forceRedRule,
        Symbol("advisoryRule") -> entity.advisoryRule,
        Symbol("createdBy") -> entity.createdBy,
        Symbol("createdAt") -> entity.createdAt,
        Symbol("updatedBy") -> entity.updatedBy,
        Symbol("updatedAt") -> entity.updatedAt,
        Symbol("reason") -> entity.reason
      )
    )
    SQL(s"""insert into $tableName(
      rule_type,
      pattern,
      replacement,
      category,
      tags,
      description,
      notes,
      google_sheet_id,
      force_red_rule,
      advisory_rule,
      created_at,
      created_by,
      updated_at,
      updated_by,
      reason
    ) values (
      {ruleType},
      {pattern},
      {replacement},
      {category},
      {tags},
      {description},
      {notes},
      {googleSheetId},
      {forceRedRule},
      {advisoryRule},
      {createdAt},
      {createdBy},
      {updatedAt},
      {updatedBy},
      {reason}
    )""").batchByName(params.toSeq: _*).apply[List]()
  }

  def destroyAll()(implicit session: DBSession = autoSession): Int = {
    withSQL {
      delete.from(DbRuleLive)
    }.update().apply()
  }
}
