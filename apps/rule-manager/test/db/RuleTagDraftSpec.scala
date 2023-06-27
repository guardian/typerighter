package db

import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scalikejdbc.scalatest.AutoRollback
import scalikejdbc._

class RuleTagDraftSpec extends FixtureAnyFlatSpec with Matchers with AutoRollback with DBTest {
  val rt = RuleTagDraft.syntax("rt")

  override def fixture(implicit session: DBSession) = {
    // Todo: DRY out fixtures
    sql"""
    ALTER SEQUENCE rules_id_seq RESTART WITH 1;
    ALTER SEQUENCE tags_id_seq RESTART WITH 1;
   """.update().apply()
    val testRuleId =
      sql"insert into rules_draft (rule_type, pattern, replacement, category, description, ignore, notes, external_id, force_red_rule, advisory_rule, created_by, updated_by) values (${"regex"}, ${"pattern"}, ${"replacement"}, ${"category"}, ${"description"}, false, ${"notes"}, ${"externalId"}, false, false, 'test.user', 'test.user')"
        .updateAndReturnGeneratedKey()
        .apply()
        .toInt
    val testTagId = sql"insert into tags (name) values (${"testTag"})".update().apply()
    sql"insert into rule_tag_draft (rule_id, tag_id) values ($testRuleId, $testTagId)"
      .update()
      .apply()
  }

  behavior of "Rule Draft - Tag join table"

  it should "find by composite key" in { implicit session =>
    val maybeFound = RuleTagDraft.find(1, 1)
    maybeFound.isDefined should be(true)
  }

  it should "find tags by rule" in { implicit session =>
    val maybeFound = RuleTagDraft.findTagsByRule(1)
    maybeFound should be(List(1))
  }

  it should "find rules by tag" in { implicit session =>
    val maybeFound = RuleTagDraft.findRulesByTag(1)
    maybeFound should be(List(1))
  }

  it should "find all records" in { implicit session =>
    val allResults = RuleTagDraft.findAll()
    allResults should be(List(RuleTagDraft(1, 1)))
  }

  it should "create new 'rule draft -> tag' join record" in { implicit session =>
    val newTag = Tags.create("New tag").get
    val created = RuleTagDraft
      .create(ruleId = 1, tagId = newTag.id.get)
      .get

    created shouldBe RuleTagDraft(1, newTag.id.get)
  }

  it should "destroy a record" in { implicit session =>
    val toDestroy = RuleTagDraft.findAll().head
    val deleted = RuleTagDraft.destroy(toDestroy)
    deleted should be(1)
    val result = RuleTagDraft.findTagsByRule(1)
    result should be(List())
  }

  it should "perform batch insert" in { implicit session =>
    val entities = RuleTagDraft.findAll()
    entities.foreach(e => RuleTagDraft.destroy(e))
    RuleTagDraft.batchInsert(entities)
    val entitiesAfterBatchInsert = RuleTagDraft.findAll()
    entitiesAfterBatchInsert should be(entities)
  }
}
