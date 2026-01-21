package db

import db.DbRule._
import model.PaginatedResponse
import model.{CreateRuleForm, UpdateRuleForm}
import play.api.libs.json.{Format, Json}
import play.api.mvc.Result
import play.api.mvc.Results.{InternalServerError, NotFound}
import scalikejdbc._

import java.time.OffsetDateTime
import scala.util.{Failure, Success, Try}
import utils.StringHelpers
import service.RuleManager.RuleType
import java.time.temporal.ChronoUnit

case class DbRuleDraft(
    id: Option[Int],
    ruleType: String,
    pattern: Option[String] = None,
    replacement: Option[String] = None,
    category: Option[String] = None,
    tags: List[Int] = List.empty,
    title: Option[String] = None,
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
    isArchived: Boolean,
    ruleOrder: Int,
    hasUnpublishedChanges: Boolean
) extends DbRuleCommon {

  def toLive(reason: String, isActive: Boolean = false): DbRuleLive = {
    id match {
      case None =>
        throw new Exception(
          s"Attempted to make live rule for rule with externalId $externalId, but the rule did not have an id"
        )
      case Some(_) =>
        DbRuleLive(
          ruleType = ruleType,
          pattern = pattern,
          replacement = replacement,
          category = category,
          tags = tags,
          title = title,
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
          ruleOrder = ruleOrder,
          isActive = isActive
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
      title = rs.stringOpt("title"),
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
      isArchived = rs.boolean("is_archived"),
      ruleOrder = rs.int("rule_order"),
      hasUnpublishedChanges = rs.boolean("has_unpublished_changes")
    )
  }

  case class PaginatedRow(rule: DbRuleDraft, ruleCount: Int, pageCount: Int)

  /** Returns a tuple with the rule, the total number of rules, and the total number of pages.
    */
  def fromPaginatedRow(rs: WrappedResultSet): PaginatedRow = {
    PaginatedRow(fromRow(rs), rs.int("rule_count"), rs.int("page_count"))
  }

  def withUser(
      id: Option[Int],
      ruleType: String,
      pattern: Option[String] = None,
      replacement: Option[String] = None,
      category: Option[String] = None,
      tags: List[Int] = List.empty,
      title: Option[String] = None,
      description: Option[String] = None,
      ignore: Boolean,
      notes: Option[String] = None,
      externalId: Option[String] = None,
      forceRedRule: Option[Boolean] = None,
      advisoryRule: Option[Boolean] = None,
      user: String,
      ruleOrder: Int
  ) = {
    val createdAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS)
    DbRuleDraft(
      id,
      ruleType,
      pattern,
      replacement,
      category,
      tags,
      title,
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
      isArchived = false,
      ruleOrder = ruleOrder,
      hasUnpublishedChanges = false
    )
  }

  val rd = DbRuleDraft.syntax("rd")
  val rl = DbRuleLive.syntax("rl")
  val rt = RuleTagDraft.syntax("rt")

  val tagColumn =
    sqls"COALESCE(ARRAY_AGG(${rt.tagId}) FILTER (WHERE ${rt.tagId} IS NOT NULL), '{}') AS tags"

  val isPublishedColumn = sqls"${rl.externalId} IS NOT NULL AS is_published"

  val hasUnpublishedChangesColumn =
    sqls"(${rl.revisionId} IS NOT NULL AND ${rl.revisionId} < ${rd.revisionId}) AS has_unpublished_changes"

  val draftRuleColumns = SQLSyntax.createUnsafely(
    rd.columns.filter(_.value != "tags").map(c => s"${rd.tableAliasName}.${c.value}").mkString(", ")
  )

  override val autoSession = AutoSession

  def find(id: Int)(implicit session: DBSession = autoSession): Option[DbRuleDraft] = {
    withSQL {
      select(draftRuleColumns, isPublishedColumn, hasUnpublishedChangesColumn, tagColumn)
        .from(DbRuleDraft as rd)
        .leftJoin(DbRuleLive as rl)
        .on(sqls"${rd.externalId} = ${rl.externalId} and ${rl.isActive} = true")
        .leftJoin(RuleTagDraft as rt)
        .on(rd.id, rt.ruleId)
        .where
        .eq(rd.id, id)
        .groupBy(draftRuleColumns, rl.externalId, rl.revisionId)
        .orderBy(rd.ruleOrder)
    }.map(DbRuleDraft.fromRow)
      .single()
      .apply()
  }

  def findRules(ids: List[Int])(implicit session: DBSession = autoSession): List[DbRuleDraft] = {
    withSQL {
      select(draftRuleColumns, isPublishedColumn, hasUnpublishedChangesColumn, tagColumn)
        .from(DbRuleDraft as rd)
        .leftJoin(DbRuleLive as rl)
        .on(sqls"${rd.externalId} = ${rl.externalId} and ${rl.isActive} = true")
        .leftJoin(RuleTagDraft as rt)
        .on(rd.id, rt.ruleId)
        .where
        .in(rd.id, ids)
        .groupBy(draftRuleColumns, rl.externalId, rl.revisionId)
        .orderBy(rd.ruleOrder)
    }
      .map(DbRuleDraft.fromRow)
      .list()
      .apply()
  }

  def findAll()(implicit session: DBSession = autoSession): List[DbRuleDraft] = {
    withSQL {
      select(draftRuleColumns, isPublishedColumn, hasUnpublishedChangesColumn, tagColumn)
        .from(DbRuleDraft as rd)
        .leftJoin(DbRuleLive as rl)
        .on(sqls"${rd.externalId} = ${rl.externalId} and ${rl.isActive} = true")
        .leftJoin(RuleTagDraft as rt)
        .on(rd.id, rt.ruleId)
        .groupBy(draftRuleColumns, rl.externalId, rl.revisionId)
        .orderBy(rd.ruleOrder)
    }.map(DbRuleDraft.fromRow)
      .list()
      .apply()
  }

  /** Search for rules.
    *
    * `sortBy` expects a list of columns prepended by + or - to signify direction, e.g.
    * List("+pattern", "-description")
    */
  def searchRules(
      page: Int,
      maybeWord: Option[String] = None,
      tags: List[Int] = List.empty,
      ruleTypes: List[String] = List.empty,
      sortBy: List[String] = List.empty,
      pageSize: Int = 50
  )(implicit
      session: DBSession = autoSession
  ): PaginatedResponse[DbRuleDraft] = {
    val isFilteringByAnyCondition = maybeWord.nonEmpty ||
      tags.nonEmpty ||
      ruleTypes.nonEmpty

    val coalescedCols =
      sqls"""
        coalesce(${rd.column("pattern")}, '') || ' ' ||
        coalesce(${rd.column("description")}, '') || ' ' ||
        coalesce(${rd.column("replacement")}, '')
      """

    val (searchClause, searchOrderClause) = maybeWord
      .map { word =>
        (
          Some(sqls"$coalescedCols ILIKE ${s"%$word%"}"),
          Some(sqls"similarity($coalescedCols, $word) DESC"),
        )
      }
      .getOrElse((None, None))

    val tagFilterClause = tags match {
      case Nil  => None
      case tags => Some(sqls"${rt.tagId} IN ($tags)")
    }

    val ruleTypeFilterClause = ruleTypes match {
      case Nil       => None
      case ruleTypes => Some(sqls"${rd.ruleType} IN ($ruleTypes)")
    }

    val condition =
      searchClause.toList ++ tagFilterClause.toList ++ ruleTypeFilterClause.toList match {
        case Nil => sqls.empty
        case clauses =>
          sqls"WHERE ${sqls.join(clauses, sqls"AND")}"
      }

    val orderByClause = {
      val defaultPatternOrder = if (!isFilteringByAnyCondition && sortBy.isEmpty) {
        List(sqls"${rd.updatedAt} DESC")
      } else List.empty
      val orderStmts = sortBy.map { sortByStr =>
        val colName = StringHelpers.camelToSnakeCase(sortByStr.slice(1, sortByStr.length))
        val col = rd.column(colName)
        sortByStr.slice(0, 1) match {
          // Indexes for sort order should reflect the `left()` expression.
          case "+" => sqls"left($col, 20) ASC"
          case "-" => sqls"left($col, 20) DESC"
        }
      } ++ searchOrderClause ++ defaultPatternOrder

      if (orderStmts.nonEmpty)
        sqls"ORDER BY ${sqls.join(orderStmts, sqls",")}"
      else
        sqls.empty
    }

    val countStmt = if (isFilteringByAnyCondition) {
      sqls"COUNT(*) OVER () AS rule_count"
    } else {
      // If we're not filtering, use the table statistics to count rows rather
      // than count(*), which is slow when we're counting the entire table.
      sqls"""
        (SELECT n_live_tup
          FROM pg_stat_all_tables
        WHERE relname = ${DbRuleDraft.tableName}) as rule_count
          """
    }

    val searchStmt = sql"""
        SELECT
          $draftRuleColumns,
          $isPublishedColumn,
          $hasUnpublishedChangesColumn,
          rule_count,
          CEIL(rule_count / $pageSize) as page_count,
          $tagColumn
        FROM (
        SELECT
            $draftRuleColumns,
            $countStmt
          FROM ${DbRuleDraft.as(rd)}
          ${if (tags.isEmpty) sqls.empty else sqls"""
             LEFT JOIN ${RuleTagDraft.as(rt)} ON ${rd.id} = ${rt.ruleId}
          """}
          $condition
          $orderByClause
          LIMIT $pageSize
          OFFSET ${(page - 1) * pageSize}
        ) as ${SQLSyntax.createUnsafely(rd.tableAliasName)}
          LEFT JOIN ${DbRuleLive.as(rl)}
            ON ${rd.externalId} = ${rl.externalId} and ${rl.isActive} = true
          LEFT JOIN ${RuleTagDraft.as(rt)}
            ON ${rd.id} = ${rt.ruleId}
          GROUP BY
            $draftRuleColumns,
            ${rl.externalId},
            ${rl.revisionId},
            rule_count,
            page_count
          $orderByClause
      """

    val data =
      searchStmt
        .map(rs => (DbRuleDraft.fromRow(rs), rs.int("page_count"), rs.int("rule_count")))
        .list()
        .apply()

    val (maxPages, total) = data.headOption
      .map { case (r, pageCount, ruleCount) =>
        (pageCount, ruleCount)
      }
      .getOrElse((1, 0))

    val rules = data.map(_._1)

    PaginatedResponse(rules, pageSize, page, maxPages, total)
  }

  def findAllDictionaryRules()(implicit session: DBSession = autoSession): List[DbRuleDraft] = {
    withSQL {
      select(draftRuleColumns, isPublishedColumn, hasUnpublishedChangesColumn, tagColumn)
        .from(DbRuleDraft as rd)
        .leftJoin(DbRuleLive as rl)
        .on(sqls"${rd.externalId} = ${rl.externalId} and ${rl.isActive} = true")
        .leftJoin(RuleTagDraft as rt)
        .on(rd.id, rt.ruleId)
        .where
        .eq(rd.ruleType, "dictionary")
        .groupBy(draftRuleColumns, rl.externalId, rl.revisionId)
        .orderBy(rd.ruleOrder)
    }
      .fetchSize(1000)
      .map(DbRuleDraft.fromRow)
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

  def getLatestRuleOrder()(implicit session: DBSession = autoSession): Int = {
    SQL(s"""
         |SELECT rule_order
         |    FROM $tableName
         |    ORDER BY rule_order DESC
         |    LIMIT 1
         |""".stripMargin).map(_.int(1)).single().apply().getOrElse(0)
  }

  def create(
      ruleType: String,
      pattern: Option[String] = None,
      replacement: Option[String] = None,
      category: Option[String] = None,
      title: Option[String] = None,
      description: Option[String] = None,
      ignore: Boolean,
      notes: Option[String] = None,
      forceRedRule: Option[Boolean] = None,
      advisoryRule: Option[Boolean] = None,
      externalId: Option[String] = None,
      user: String,
      isArchived: Boolean = false,
      tags: List[Int] = List.empty
  )(implicit session: DBSession = autoSession): Try[DbRuleDraft] = {
    val latestRuleOrder = getLatestRuleOrder()

    val externalIdCol = externalId
      .orElse {
        ruleType match {
          case RuleType.languageToolCore => Some("CHANGE_ME")
          case _                         => None
        }
      }
      .map((column.externalId -> _))
      .toList

    val columnUpdates = List(
      column.ruleType -> ruleType,
      column.pattern -> pattern,
      column.replacement -> replacement,
      column.category -> category,
      column.title -> title,
      column.description -> description,
      column.ignore -> ignore,
      column.notes -> notes,
      column.forceRedRule -> forceRedRule,
      column.advisoryRule -> advisoryRule,
      column.createdBy -> user,
      column.updatedBy -> user,
      column.isArchived -> isArchived,
      column.ruleOrder -> (latestRuleOrder + 1)
    ) ++ externalIdCol

    val id = withSQL {
      insert
        .into(DbRuleDraft)
        .namedValues(
          columnUpdates: _*
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
      title = formRule.title,
      description = formRule.description,
      ignore = formRule.ignore,
      notes = formRule.notes,
      externalId = formRule.externalId,
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
          tags = formRule.tags,
          title = formRule.title,
          description = formRule.description,
          advisoryRule = formRule.advisoryRule,
          externalId = formRule.externalId.orElse(existingRule.externalId)
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

  def batchUpdate(ids: List[Int], category: Option[String], tags: Option[List[Int]], user: String)(
      implicit session: DBSession = autoSession
  ): Try[List[DbRuleDraft]] = {
    Try {
      val fieldsToUpdate = List(
        column.updatedBy -> user,
        column.revisionId -> sqls"${column.revisionId} + 1"
      ) ++ (if (category.isDefined) List(column.category -> category.get) else Nil)

      val updateColumns = update(DbRuleDraft)
        .set(fieldsToUpdate: _*)
        .where
        .in(column.id, ids)

      tags.foreach { tagList =>
        ids.foreach(RuleTagDraft.destroyForRule)
        val newTags = ids.flatMap(id => tagList.map(tagId => RuleTagDraft(id, tagId)))
        RuleTagDraft.batchInsert(newTags)
      }

      withSQL(updateColumns).update().apply()
      val rules = findRules(ids)
      rules
    }
  }

  def batchInsert(
      entities: collection.Seq[DbRuleDraft],
      hasNoExternalId: Boolean = false
  )(implicit session: DBSession = autoSession): List[Int] = {
    val params: collection.Seq[Seq[(Symbol, Any)]] = entities.map(entity => {
      val baseFields = Seq(
        Symbol("ruleType") -> entity.ruleType,
        Symbol("pattern") -> entity.pattern,
        Symbol("replacement") -> entity.replacement,
        Symbol("category") -> entity.category,
        Symbol("title") -> entity.title,
        Symbol("description") -> entity.description,
        Symbol("ignore") -> entity.ignore,
        Symbol("notes") -> entity.notes,
        Symbol("forceRedRule") -> entity.forceRedRule,
        Symbol("advisoryRule") -> entity.advisoryRule,
        Symbol("createdBy") -> entity.createdBy,
        Symbol("createdAt") -> entity.createdAt,
        Symbol("updatedBy") -> entity.updatedBy,
        Symbol("updatedAt") -> entity.updatedAt,
        Symbol("isArchived") -> entity.isArchived,
        Symbol("ruleOrder") -> entity.ruleOrder
      )
      hasNoExternalId match {
        case true  => baseFields
        case false => (Symbol("externalId") -> entity.externalId) +: baseFields
      }
    })
    SQL(s"""insert into $tableName(
      ${if (hasNoExternalId) "" else "external_id,"}
      rule_type,
      pattern,
      replacement,
      category,
      title,
      description,
      ignore,
      notes,
      force_red_rule,
      advisory_rule,
      created_by,
      created_at,
      updated_by,
      updated_at,
      is_archived,
      rule_order
    ) values (
      ${if (hasNoExternalId) "" else "{externalId},"}
      {ruleType},
      {pattern},
      {replacement},
      {category},
      {title},
      {description},
      {ignore},
      {notes},
      {forceRedRule},
      {advisoryRule},
      {createdBy},
      {createdAt},
      {updatedBy},
      {updatedAt},
      {isArchived},
      {ruleOrder}
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

  def save(entity: DbRuleDraft, user: String, overrideRevisionId: Boolean = false)(implicit
      session: DBSession = autoSession
  ): Try[DbRuleDraft] = {
    val id = withSQL {
      update(DbRuleDraft)
        .set(
          column.ruleType -> entity.ruleType,
          column.pattern -> entity.pattern,
          column.replacement -> entity.replacement,
          column.category -> entity.category,
          column.title -> entity.title,
          column.description -> entity.description,
          column.ignore -> entity.ignore,
          column.notes -> entity.notes,
          column.externalId -> entity.externalId,
          column.forceRedRule -> entity.forceRedRule,
          column.advisoryRule -> entity.advisoryRule,
          column.createdAt -> entity.createdAt,
          column.createdBy -> entity.createdBy,
          column.updatedAt -> OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS),
          column.updatedBy -> user,
          column.isArchived -> entity.isArchived,
          column.revisionId -> (overrideRevisionId match {
            case false => sqls"${column.revisionId} + 1"
            case true  => sqls"${entity.revisionId}"
          })
        )
        .where
        .eq(column.id, entity.id)
    }.updateAndReturnGeneratedKey().apply().toInt

    RuleTagDraft.destroyForRule(id)
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

  def destroyDictionaryRules()(implicit session: DBSession = autoSession): Int = {
    withSQL {
      delete
        .from(DbRuleDraft as rd)
        .where
        .eq(rd.ruleType, "dictionary")
    }.update().apply()
  }

  def destroyAll()(implicit session: DBSession = autoSession): Int = {
    withSQL {
      delete.from(DbRuleDraft)
    }.update().apply()
  }
}
