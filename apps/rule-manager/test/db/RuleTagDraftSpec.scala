package db

import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scalikejdbc.scalatest.AutoRollback
import scalikejdbc._

class RuleTagDraftSpec extends FixtureAnyFlatSpec with Matchers with AutoRollback with DBTest {
  val rt = RuleTagDraft.syntax("rt")

  override def fixture(implicit session: DBSession) = {
    sql"insert into rule_tag_draft (rule_id, tag_id) values (12, 34)"
      .update()
      .apply()
  }

  behavior of "Rule Draft - Tag join table"

  it should "find by composite key" in { implicit session =>
    val maybeFound = RuleTagDraft.find(12, 34)
    maybeFound.isDefined should be(true)
  }

  it should "find tags by rule" in { implicit session =>
    val maybeFound = RuleTagDraft.findTagsByRule(12)
    maybeFound should be(List(34))
  }

  it should "find rules by tag" in { implicit session =>
    val maybeFound = RuleTagDraft.findRulesByTag(34)
    maybeFound should be(List(12))
  }

  it should "find all records" in { implicit session =>
    val allResults = RuleTagDraft.findAll()
    allResults should be(List(RuleTag(12, 34)))
  }

  it should "count all records" in { implicit session =>
    val count = RuleTagDraft.countAll()
    count should be > (0L)
  }

  it should "count by where clauses" in { implicit session =>
    val count = RuleTagDraft.countBy(sqls.eq(rt.rule_id, 12))
    count should be > (0L)
  }

  it should "create new 'rule draft -> tag' join record" in { implicit session =>
    val created = RuleTagDraft
      .create(ruleId = 56, tagId = 78)
      .get

    created.rule_id shouldBe 56
    created.tag_id shouldBe 78
  }

  it should "destroy a record" in { implicit session =>
    val created = RuleTagDraft
      .create(ruleId = 90, tagId = 12)
      .get
    val deleted = RuleTagDraft.destroy(created)
    deleted should be(1)
    val result = RuleTagDraft.findTagsByRule(90)
    result should be (List())
  }

  it should "perform batch insert" in { implicit session =>
    val entities = RuleTagDraft.findAll()
    entities.foreach(e => RuleTagDraft.destroy(e))
    val batchInserted = RuleTagDraft.batchInsert(entities)
    batchInserted.size should be > (0)
  }
}
