package db

import scalikejdbc._

import scala.util.Try

case class Rules(
    id: Int,
    ruleType: String,
    pattern: String,
    replacement: Option[String] = None,
    category: Option[String] = None,
    tags: Option[String] = None,
    description: Option[String] = None,
    ignore: Boolean,
    notes: Option[String] = None,
    googleSheetId: Option[String] = None,
    forceRedRule: Option[Boolean] = None,
    advisoryRule: Option[Boolean] = None
) {

  def save()(implicit session: DBSession = Rules.autoSession): Try[Rules] =
    Rules.save(this)(session)

  def destroy()(implicit session: DBSession = Rules.autoSession): Int = Rules.destroy(this)(session)

}

object Rules extends SQLSyntaxSupport[Rules] {

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
    "advisory_rule"
  )

  def apply(r: SyntaxProvider[Rules])(rs: WrappedResultSet): Rules = autoConstruct(rs, r)
  def apply(r: ResultName[Rules])(rs: WrappedResultSet): Rules = autoConstruct(rs, r)

  val r = Rules.syntax("r")

  override val autoSession = AutoSession

  def find(id: Int)(implicit session: DBSession = autoSession): Option[Rules] = {
    withSQL {
      select.from(Rules as r).where.eq(r.id, id)
    }.map(Rules(r.resultName)).single.apply()
  }

  def findAll()(implicit session: DBSession = autoSession): List[Rules] = {
    withSQL(select.from(Rules as r)).map(Rules(r.resultName)).list.apply()
  }

  def countAll()(implicit session: DBSession = autoSession): Long = {
    withSQL(select(sqls.count).from(Rules as r)).map(rs => rs.long(1)).single.apply().get
  }

  def findBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Option[Rules] = {
    withSQL {
      select.from(Rules as r).where.append(where)
    }.map(Rules(r.resultName)).single.apply()
  }

  def findAllBy(where: SQLSyntax)(implicit session: DBSession = autoSession): List[Rules] = {
    withSQL {
      select.from(Rules as r).where.append(where)
    }.map(Rules(r.resultName)).list.apply()
  }

  def countBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Long = {
    withSQL {
      select(sqls.count).from(Rules as r).where.append(where)
    }.map(_.long(1)).single.apply().get
  }

  def create(
      ruleType: String,
      pattern: String,
      replacement: Option[String] = None,
      category: Option[String] = None,
      tags: Option[String] = None,
      description: Option[String] = None,
      ignore: Boolean,
      notes: Option[String] = None,
      googleSheetId: Option[String] = None,
      forceRedRule: Option[Boolean] = None,
      advisoryRule: Option[Boolean] = None
  )(implicit session: DBSession = autoSession): Rules = {
    val generatedKey = withSQL {
      insert
        .into(Rules)
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
          column.advisoryRule -> advisoryRule
        )
    }.updateAndReturnGeneratedKey.apply()

    Rules(
      id = generatedKey.toInt,
      ruleType = ruleType,
      pattern = pattern,
      replacement = replacement,
      category = category,
      tags = tags,
      description = description,
      ignore = ignore,
      notes = notes,
      googleSheetId = googleSheetId,
      forceRedRule = forceRedRule,
      advisoryRule = advisoryRule
    )
  }

  def batchInsert(
      entities: collection.Seq[Rules]
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
        Symbol("advisoryRule") -> entity.advisoryRule
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
      advisory_rule
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
      {advisoryRule}
    )""").batchByName(params.toSeq: _*).apply[List]()
  }

  def save(entity: Rules)(implicit session: DBSession = autoSession): Try[Rules] = {
    withSQL {
      update(Rules)
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
          column.advisoryRule -> entity.advisoryRule
        )
        .where
        .eq(column.id, entity.id)
    }.update.apply()

    find(entity.id)
      .toRight(
        new Exception(s"Error updating rule with id ${entity.id}: could not read updated rule")
      )
      .toTry
  }

  def destroy(entity: Rules)(implicit session: DBSession = autoSession): Int = {
    withSQL { delete.from(Rules).where.eq(column.id, entity.id) }.update.apply()
  }

}
