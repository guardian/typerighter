package db

import scalikejdbc._

import scala.util.{Failure, Success, Try}

case class RuleTagLive(
                        ruleExternalId: String,
                        ruleRevisionId: Int,
                        tagId: Int
)

object RuleTagLive extends SQLSyntaxSupport[RuleTagLive] {
  override val tableName = "rule_tag_live"
  override val columns = Seq("rule_external_id", "rule_revision_id", "tag_id")

  val rt = this.syntax("rt")

  def fromResultName(r: ResultName[RuleTagLive])(rs: WrappedResultSet): RuleTagLive =
    autoConstruct(rs, r)

  def find(externalId: String, revisionId: Int, tagId: Int)(implicit session: DBSession = autoSession): Option[RuleTagLive] = {
    withSQL {
      select.from(this as rt)
        .where.eq(rt.ruleExternalId, externalId)
        .and.eq(rt.ruleRevisionId, revisionId)
        .and.eq(rt.tagId, tagId)
    }.map(this.fromResultName(rt.resultName)).single().apply()
  }

  def findTagsByRule(externalId: String, revisionId: Int)(implicit session: DBSession = autoSession): List[Int] = {
    withSQL {
      select.from(this as rt)
        .where.eq(rt.ruleExternalId, externalId)
        .and.eq(rt.ruleRevisionId, revisionId)
    }.map(this.fromResultName(rt.resultName)).list().apply().map(rt => rt.tagId)
  }

  def findRulesByTag(tagId: Int)(implicit session: DBSession = autoSession): List[(String, Int)] = {
    withSQL {
      select.from(this as rt).where.eq(rt.tagId, tagId)
    }.map(this.fromResultName(rt.resultName)).list().apply().map(rt => (rt.ruleExternalId, rt.ruleRevisionId))
  }

  def findAll()(implicit session: DBSession = autoSession): List[RuleTagLive] = {
    withSQL(select.from(this as rt).orderBy(rt.ruleExternalId))
      .map(this.fromResultName(rt.resultName))
      .list()
      .apply()
  }

  def create(
    externalId: String,
    revisionId: Int,
    tagId: Int
  )(implicit session: DBSession = autoSession): Try[RuleTagLive] = {
    withSQL {
      insert
        .into(this)
        .namedValues(
          column.ruleExternalId -> externalId,
          column.ruleRevisionId -> revisionId,
          column.tagId -> tagId
        )
    }.update().apply()
    find(externalId, revisionId, tagId) match {
      case Some(ruleTagLive) => Success(ruleTagLive)
      case None =>
        Failure(
          new Exception(
            s"Attempted to create a RuleTagLive with id $tagId, but no result found attempting to read it back"
          )
        )
    }
  }

  def batchInsert(
      entities: collection.Seq[RuleTagLive]
  )(implicit session: DBSession = autoSession): List[Int] = {
    val params: collection.Seq[Seq[(Symbol, Any)]] = entities.map(entity =>
      Seq(
        Symbol("rule_external_id") -> entity.ruleExternalId,
        Symbol("rule_revision_id") -> entity.ruleRevisionId,
        Symbol("tag_id") -> entity.tagId
      )
    )
    SQL(s"""insert into $tableName(
      rule_external_id,
      rule_revision_id,
      tag_id
    ) values (
      {rule_external_id},
      {rule_revision_id},
      {tag_id}
    )""").batchByName(params.toSeq: _*).apply[List]()
  }

  def destroy(entity: RuleTagLive)(implicit session: DBSession = autoSession): Int = {
    withSQL {
      delete
        .from(this)
        .where
        .eq(column.ruleExternalId, entity.ruleExternalId)
        .and
        .eq(column.ruleRevisionId, entity.ruleRevisionId)
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
