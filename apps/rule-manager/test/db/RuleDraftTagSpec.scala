package db

import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scalikejdbc.scalatest.AutoRollback
import scalikejdbc._

class RuleDraftTagSpecSpec extends FixtureAnyFlatSpec with Matchers with AutoRollback with DBTest {
  val rt = RuleDraftTag.syntax("rt")

  override def fixture(implicit session: DBSession) = {
    sql"insert into rules_draft_tags (rule_id, tag_id) values (${123} ${456})"
      .update()
      .apply()
  }

  behavior of "Rule Draft - Tag join table"

  it should "find by composite key" in { implicit session =>
    val maybeFound = RuleDraftTag.find(12, 34)
    maybeFound.isDefined should be(true)
  }
  it should "find by rule" in { implicit session =>
    val maybeFound = RuleDraftTag.findByRule(12)
    maybeFound should be(List(34))
  }
  it should "find by tag" in { implicit session =>
    val maybeFound = RuleDraftTag.findByRule(34)
    maybeFound should be(List(12))
  }
  it should "find all records" in { implicit session =>
    val allResults = RuleDraftTag.findAll()
    allResults should be(RuleTag(12, 34))
  }
  it should "count all records" in { implicit session =>
    val count = RuleDraftTag.countAll()
    count should be > (0L)
  }
  it should "find all by where clauses" in { implicit session =>
    val results = Tags.findAllBy(sqls.eq(rt.id, 1))
    results.size should be > (0)
  }
  it should "count by where clauses" in { implicit session =>
    val count = RuleDraftTag.countBy(sqls.eq(rt.rule_id, 12))
    count should be > (0L)
  }
  it should "create new 'rule draft -> tag' join record" in { implicit session =>
    val created = RuleDraftTag
      .create(ruleId = 56, tagId = 78)
      .get

    created.rule_id shouldBe 56
    created.tag_id shouldBe 78
  }

  it should "destroy a record" in { implicit session =>
    val created = RuleDraftTag
      .create(ruleId = 90, tagId = 12)
      .get
    val deleted = RuleDraftTag.destroy(created)
    deleted should be(1)
    val shouldBeNone = RuleDraftTag.findByRule(90)
    shouldBeNone should be(None)
  }

  it should "perform batch insert" in { implicit session =>
    val entities = RuleDraftTag.findAll()
    entities.foreach(e => RuleDraftTag.destroy(e))
    val batchInserted = RuleDraftTag.batchInsert(entities)
    batchInserted.size should be > (0)
  }
}
