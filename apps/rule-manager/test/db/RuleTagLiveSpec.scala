package db

import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scalikejdbc.scalatest.AutoRollback
import scalikejdbc._

class RuleTagLiveSpec extends FixtureAnyFlatSpec with Matchers with AutoRollback with DBTest {
  val rt = RuleTagLive.syntax("rt")

  override def fixture(implicit session: DBSession) = {
    sql"insert into rule_tag_live (rule_id, tag_id) values (12, 34)"
      .update()
      .apply()
  }

  behavior of "Rule Draft - Tag join table"

  it should "find by composite key" in { implicit session =>
    val maybeFound = RuleTagLive.find(12, 34)
    maybeFound.isDefined should be(true)
  }

  it should "find tags by rule" in { implicit session =>
    val maybeFound = RuleTagLive.findTagsByRule(12)
    maybeFound should be(List(34))
  }

  it should "find rules by tag" in { implicit session =>
    val maybeFound = RuleTagLive.findRulesByTag(34)
    maybeFound should be(List(12))
  }

  it should "find all records" in { implicit session =>
    val allResults = RuleTagLive.findAll()
    allResults should be(List(RuleTag(12, 34)))
  }

  it should "count all records" in { implicit session =>
    val count = RuleTagLive.countAll()
    count should be > (0L)
  }

  it should "count by where clauses" in { implicit session =>
    val count = RuleTagLive.countBy(sqls.eq(rt.rule_id, 12))
    count should be > (0L)
  }

  it should "create new 'rule draft -> tag' join record" in { implicit session =>
    val created = RuleTagLive
      .create(ruleId = 56, tagId = 78)
      .get

    created.rule_id shouldBe 56
    created.tag_id shouldBe 78
  }

  it should "destroy a record" in { implicit session =>
    val created = RuleTagLive
      .create(ruleId = 90, tagId = 12)
      .get
    val deleted = RuleTagLive.destroy(created)
    deleted should be(1)
    val result = RuleTagLive.findTagsByRule(90)
    result should be(List())
  }

  it should "perform batch insert" in { implicit session =>
    val entities = RuleTagLive.findAll()
    entities.foreach(e => RuleTagLive.destroy(e))
    val batchInserted = RuleTagLive.batchInsert(entities)
    batchInserted.size should be > (0)
  }
}
