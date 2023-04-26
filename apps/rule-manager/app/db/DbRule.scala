package db

import model.{CreateRuleForm, UpdateRuleForm}
import play.api.libs.json.{Format, JsValue, Json}
import play.api.mvc.Result
import play.api.mvc.Results.{InternalServerError, NotFound}
import scalikejdbc._
import scalikejdbc.interpolation.SQLSyntax.distinct

import scala.util.{Failure, Try}

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
    advisoryRule: Option[Boolean] = None
) {

  def save()(implicit session: DBSession = DbRule.autoSession): Try[DbRule] =
    DbRule.save(this)(session)

  def destroy()(implicit session: DBSession = DbRule.autoSession): Int =
    DbRule.destroy(this)(session)
}

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
    "advisory_rule"
  )

  def fromSyntaxProvider(r: SyntaxProvider[DbRule])(rs: WrappedResultSet): DbRule =
    autoConstruct(rs, r)
  def fromResultName(r: ResultName[DbRule])(rs: WrappedResultSet): DbRule = autoConstruct(rs, r)

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

  def findAllTags()(implicit session: DBSession = autoSession): List[String] = {
    val tags = withSQL(select(distinct(r.tags)).from(DbRule as r).orderBy(r.tags))
      .map(DbRule.fromResultName(r.resultName))
      .list
      .apply()
      .flatMap(_.tags.map(string => string.split(",").toList))
    tags.flatten.distinct
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
      advisoryRule: Option[Boolean] = None
  )(implicit session: DBSession = autoSession): DbRule = {
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
          column.advisoryRule -> advisoryRule
        )
    }.updateAndReturnGeneratedKey.apply()

    DbRule(
      id = Some(generatedKey.toInt),
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

  def createFromFormRule(formRule: CreateRuleForm)(implicit session: DBSession = autoSession) = {
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
      formRule.advisoryRule
    )
  }

  def updateFromFormRule(
      formRule: UpdateRuleForm,
      id: Int
  )(implicit session: DBSession = autoSession): Either[Result, DbRule] = {
    val updatedRule = DbRule
      .find(id)
      .toRight(NotFound("Rule not found matching ID"))
      .map(existingRule =>
        new DbRule(
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
        DbRule.save(dbRule).toEither match {
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

  def save(entity: DbRule)(implicit session: DBSession = autoSession): Try[DbRule] = {
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
          column.advisoryRule -> entity.advisoryRule
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
    withSQL { delete.from(DbRule).where.eq(column.id, entity.id) }.update.apply()
  }

  def destroyAll()(implicit session: DBSession = autoSession): Int = {
    withSQL {
      delete.from(DbRule)
    }.update.apply()
  }

  def toJson(dbRule: DbRule): JsValue = {
    Json.toJson(
      Map(
        "id" -> Json.toJson(dbRule.id),
        "ruleType" -> Json.toJson(dbRule.ruleType),
        "pattern" -> Json.toJson(dbRule.pattern),
        "replacement" -> Json.toJson(dbRule.replacement),
        "category" -> Json.toJson(dbRule.category),
        "tags" -> Json.toJson(dbRule.tags),
        "description" -> Json.toJson(dbRule.description),
        "ignore" -> Json.toJson(dbRule.ignore),
        "notes" -> Json.toJson(dbRule.notes),
        "googleSheetId" -> Json.toJson(dbRule.googleSheetId),
        "forceRedRule" -> Json.toJson(dbRule.forceRedRule),
        "advisoryRule" -> Json.toJson(dbRule.advisoryRule)
      )
    )
  }
}
