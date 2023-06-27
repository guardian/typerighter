package db

import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scalikejdbc.scalatest.AutoRollback
import scalikejdbc._



class RuleTagLiveSpec extends FixtureAnyFlatSpec with Matchers with AutoRollback with DBTest {
  val rt = RuleTagLive.syntax("rt")
  val initialExternalId = "googleSheetId"
  val initialRevisionId = 0

  override def fixture(implicit session: DBSession) = {
    sql"""
        ALTER SEQUENCE rules_id_seq RESTART WITH 1;
        ALTER SEQUENCE tags_id_seq RESTART WITH 1;
    """.update().apply()
    sql"insert into rules_live (rule_type, pattern, replacement, category, description, notes, external_id, force_red_rule, advisory_rule, created_by, updated_by, is_active, rule_order, revision_id) values ('regex', 'pattern', 'replacement', 'category', 'description', 'notes', $initialExternalId, false, false, 'test.user', 'test.user', true, 1, $initialRevisionId)"
      .update()
      .apply()
    val testTagId = sql"insert into tags (name) values ('testTag')".update().apply()
    sql"insert into rule_tag_live (rule_revision_id, rule_external_id, tag_id) values ($initialExternalId, $initialRevisionId, $testTagId)"
      .update()
      .apply()
  }

  behavior of "Rule Draft - Tag join table"

  it should "find by composite key" in { implicit session =>
    val maybeFound = RuleTagLive.find(initialExternalId, initialRevisionId, 1)
    maybeFound.isDefined should be(true)
  }

  it should "find tags by rule" in { implicit session =>
    val maybeFound = RuleTagLive.findTagsByRule(initialExternalId, initialRevisionId)
    maybeFound should be(List(1))
  }

  it should "find rules by tag" in { implicit session =>
    val maybeFound = RuleTagLive.findRulesByTag(1)
    maybeFound should be(List(1))
  }

  it should "find all records" in { implicit session =>
    val allResults = RuleTagLive.findAll()
    allResults should be(List(RuleTagLive(initialExternalId, initialRevisionId, 1)))
  }

  it should "create new 'rule draft -> tag' join record" in { implicit session =>
    val newTag = Tags.create("New tag").get
    val created = RuleTagLive
      .create(externalId = initialExternalId, revisionId = initialRevisionId, tagId = newTag.id.get)
      .get

    created shouldBe RuleTagLive(initialExternalId, initialRevisionId, newTag.id.get)
  }

  it should "destroy a record" in { implicit session =>
    val toDestroy = RuleTagLive.findAll().head
    val deleted = RuleTagLive.destroy(toDestroy)
    deleted should be(1)
    val result = RuleTagLive.find(
      toDestroy.ruleExternalId,
      toDestroy.ruleRevisionId,
      toDestroy.tagId
    )
    result should be(None)
  }

  it should "perform batch insert" in { implicit session =>
    val entities = RuleTagLive.findAll()
    entities.foreach(e => RuleTagLive.destroy(e))
    val batchInserted = RuleTagLive.batchInsert(entities)
    entities shouldBe batchInserted
  }
}
