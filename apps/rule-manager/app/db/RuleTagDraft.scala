package db

import scalikejdbc._

import scala.util.{Failure, Success, Try}

object RuleTagDraft extends SQLSyntaxSupport[RuleTag] {
  override val columns = Seq("rule_id", "tag_id")
  override val tableName = "rule_tag_draft"

  val rt = RuleTagDraft.syntax("rt")

  private def fromResultName(r: ResultName[RuleTag])(rs: WrappedResultSet): RuleTag =
    autoConstruct(rs, r)

  def find(ruleId: Int, tagId: Int)(implicit session: DBSession = autoSession): Option[RuleTag] = {
    withSQL {
      select.from(RuleTagDraft as rt).where.eq(rt.rule_id, ruleId).and.eq(rt.tag_id, tagId)
    }.map(RuleTagDraft.fromResultName(rt.resultName)).single().apply()
  }
  def findTagsByRule(ruleId: Int)(implicit session: DBSession = autoSession): List[Int] = {
    withSQL {
      select.from(RuleTagDraft as rt).where.eq(rt.rule_id, ruleId)
    }.map(RuleTagDraft.fromResultName(rt.resultName)).list().apply().map(rt => rt.tag_id)
  }

  def findRulesByTag(tagId: Int)(implicit session: DBSession = autoSession): List[Int] = {
    withSQL {
      select.from(RuleTagDraft as rt).where.eq(rt.tag_id, tagId)
    }.map(RuleTagDraft.fromResultName(rt.resultName)).list().apply().map(rt => rt.rule_id)
  }

  def findAll()(implicit session: DBSession = autoSession): List[RuleTag] = {
    withSQL(select.from(RuleTagDraft as rt).orderBy(rt.rule_id))
      .map(RuleTagDraft.fromResultName(rt.resultName))
      .list()
      .apply()
  }

  def countAll()(implicit session: DBSession = autoSession): Long = {
    withSQL(select(sqls.count).from(RuleTagDraft as rt)).map(rs => rs.long(1)).single().apply().get
  }

  def countBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Long = {
    withSQL {
      select(sqls.count).from(RuleTagDraft as rt).where.append(where)
    }.map(_.long(1)).single().apply().get
  }

  def create(
      ruleId: Int,
      tagId: Int
  )(implicit session: DBSession = autoSession): Try[RuleTag] = {
    val generatedKey = withSQL {
      insert
        .into(RuleTagDraft)
        .namedValues(
          column.rule_id -> ruleId,
          column.tag_id -> tagId
        )
    }.update().apply()
    find(ruleId, tagId) match {
      case Some(rule) => Success(rule)
      case None =>
        Failure(
          new Exception(
            s"Attempted to create a tag with id $generatedKey, but no result found attempting to read it back"
          )
        )
    }
  }

  def batchInsert(
      entities: collection.Seq[RuleTag]
  )(implicit session: DBSession = autoSession): List[Int] = {
    val params: collection.Seq[Seq[(Symbol, Any)]] = entities.map(entity =>
      Seq(
        Symbol("rule_id") -> entity.rule_id,
        Symbol("tag_id") -> entity.tag_id
      )
    )
    SQL(s"""insert into $tableName(
      rule_id,
      tag_id
    ) values (
      {rule_id},
      {tag_id}
    )""").batchByName(params.toSeq: _*).apply[List]()
  }

  def destroy(entity: RuleTag)(implicit session: DBSession = autoSession): Int = {
    withSQL {
      delete
        .from(RuleTagDraft)
        .where
        .eq(column.rule_id, entity.rule_id)
        .and
        .eq(column.tag_id, entity.tag_id)
    }.update().apply()
  }
}
