package db

import scalikejdbc._

import scala.util.{Failure, Success, Try}

case class RuleTag(
    rule_id: Int,
    tag_id: Int
)

trait RuleTagCommon extends SQLSyntaxSupport[RuleTag] {
  override val columns = Seq("rule_id", "tag_id")

  val rt = this.syntax("rt")

  def fromResultName(r: ResultName[RuleTag])(rs: WrappedResultSet): RuleTag =
    autoConstruct(rs, r)

  def find(ruleId: Int, tagId: Int)(implicit session: DBSession = autoSession): Option[RuleTag] = {
    withSQL {
      select.from(this as rt).where.eq(rt.rule_id, ruleId).and.eq(rt.tag_id, tagId)
    }.map(this.fromResultName(rt.resultName)).single().apply()
  }
  def findTagsByRule(ruleId: Int)(implicit session: DBSession = autoSession): List[Int] = {
    withSQL {
      select.from(this as rt).where.eq(rt.rule_id, ruleId)
    }.map(this.fromResultName(rt.resultName)).list().apply().map(rt => rt.tag_id)
  }

  def findRulesByTag(tagId: Int)(implicit session: DBSession = autoSession): List[Int] = {
    withSQL {
      select.from(this as rt).where.eq(rt.tag_id, tagId)
    }.map(this.fromResultName(rt.resultName)).list().apply().map(rt => rt.rule_id)
  }

  def findAll()(implicit session: DBSession = autoSession): List[RuleTag] = {
    withSQL(select.from(this as rt).orderBy(rt.rule_id))
      .map(this.fromResultName(rt.resultName))
      .list()
      .apply()
  }

  def countAll()(implicit session: DBSession = autoSession): Long = {
    withSQL(select(sqls.count).from(this as rt)).map(rs => rs.long(1)).single().apply().get
  }

  def countBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Long = {
    withSQL {
      select(sqls.count).from(this as rt).where.append(where)
    }.map(_.long(1)).single().apply().get
  }

  def create(
      ruleId: Int,
      tagId: Int
  )(implicit session: DBSession = autoSession): Try[RuleTag] = {
    val generatedKey = withSQL {
      insert
        .into(this)
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
        .from(this)
        .where
        .eq(column.rule_id, entity.rule_id)
        .and
        .eq(column.tag_id, entity.tag_id)
    }.update().apply()
  }

  def destroyAll()(implicit session: DBSession = autoSession): Int = {
    withSQL {
      delete.from(this)
    }.update().apply()
  }
}
