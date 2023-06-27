package db

import scalikejdbc._

import scala.util.{Failure, Success, Try}

case class RuleTagDraft(
    ruleId: Int,
    tagId: Int
)

object RuleTagDraft extends SQLSyntaxSupport[RuleTagDraft] {
  override val tableName = "rule_tag_draft"
  override val columns = Seq("rule_id", "tag_id")

  val rt = this.syntax("rt")

  def fromResultName(r: ResultName[RuleTagDraft])(rs: WrappedResultSet): RuleTagDraft =
    autoConstruct(rs, r)

  def find(ruleId: Int, tagId: Int)(implicit
      session: DBSession = autoSession
  ): Option[RuleTagDraft] = {
    withSQL {
      select.from(this as rt).where.eq(rt.ruleId, ruleId).and.eq(rt.tagId, tagId)
    }.map(this.fromResultName(rt.resultName)).single().apply()
  }
  def findTagsByRule(ruleId: Int)(implicit session: DBSession = autoSession): List[Int] = {
    withSQL {
      select.from(this as rt).where.eq(rt.ruleId, ruleId)
    }.map(this.fromResultName(rt.resultName)).list().apply().map(rt => rt.tagId)
  }

  def findRulesByTag(tagId: Int)(implicit session: DBSession = autoSession): List[Int] = {
    withSQL {
      select.from(this as rt).where.eq(rt.tagId, tagId)
    }.map(this.fromResultName(rt.resultName)).list().apply().map(rt => rt.ruleId)
  }

  def findAll()(implicit session: DBSession = autoSession): List[RuleTagDraft] = {
    withSQL(select.from(this as rt).orderBy(rt.ruleId))
      .map(this.fromResultName(rt.resultName))
      .list()
      .apply()
  }

  def create(
      ruleId: Int,
      tagId: Int
  )(implicit session: DBSession = autoSession): Try[RuleTagDraft] = {
    val generatedKey = withSQL {
      insert
        .into(this)
        .namedValues(
          column.ruleId -> ruleId,
          column.tagId -> tagId
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
      entities: collection.Seq[RuleTagDraft]
  )(implicit session: DBSession = autoSession): List[Int] = {
    val params: collection.Seq[Seq[(Symbol, Any)]] = entities.map(entity =>
      Seq(
        Symbol("rule_id") -> entity.ruleId,
        Symbol("tag_id") -> entity.tagId
      )
    )
    SQL(s"""insert into $tableName(
      rule_id,
      tag_id
    ) values (
      {rule_id},
      {tag_id}
    ) ON CONFLICT DO NOTHING""").batchByName(params.toSeq: _*).apply[List]()
  }

  def destroy(entity: RuleTagDraft)(implicit session: DBSession = autoSession): Int = {
    withSQL {
      delete
        .from(this)
        .where
        .eq(column.ruleId, entity.ruleId)
        .and
        .eq(column.tagId, entity.tagId)
    }.update().apply()
  }

  def destroyAll()(implicit session: DBSession = autoSession): Int = {
    withSQL {
      delete.from(this)
    }.update().apply()
  }
}
