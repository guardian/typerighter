package db

import db.DbRule._
import play.api.libs.json.{Format, Json}
import scalikejdbc._

import java.time.ZonedDateTime

case class LiveDbRule(
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
) extends DbRuleFields
    with LiveDbRuleFields

object LiveDbRule extends SQLSyntaxSupport[LiveDbRule] {
  implicit val format: Format[LiveDbRule] = Json.format[LiveDbRule]

  override val tableName = "rules_live"

  override val columns: Seq[String] = liveDbColumns

  def fromResultName(r: ResultName[LiveDbRule])(rs: WrappedResultSet): LiveDbRule =
    autoConstruct(rs, r)

  val r = LiveDbRule.syntax("r")

  override val autoSession = AutoSession

  def find(id: Int)(implicit session: DBSession = autoSession): Option[LiveDbRule] = {
    withSQL {
      select.from(LiveDbRule as r).where.eq(r.id, id)
    }.map(LiveDbRule.fromResultName(r.resultName)).single().apply()
  }

  def findAll()(implicit session: DBSession = autoSession): List[LiveDbRule] = {
    withSQL(select.from(LiveDbRule as r).orderBy(r.id))
      .map(LiveDbRule.fromResultName(r.resultName))
      .list()
      .apply()
  }

  def batchInsert(
      entities: collection.Seq[LiveDbRule]
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
        Symbol("updatedBy") -> entity.updatedBy,
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
      created_by,
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
      {createdBy},
      {updatedBy},
      {reason}
    )""").batchByName(params.toSeq: _*).apply[List]()
  }
}
