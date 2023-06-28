package db

import org.scalatest.matchers.should.Matchers

class RuleTagLiveSpec extends RuleFixture with Matchers with DBTest {
  val rt = RuleTagLive.syntax("rt")
  val initialExternalId = "googleSheetId"
  val initialRevisionId = 0

  behavior of "Rule Live - Tag join table"

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
    maybeFound should be(List(("googleSheetId", 0)))
  }

  it should "find all records" in { implicit session =>
    val allResults = RuleTagLive.findAll()
    allResults should be(List(RuleTagLive(initialExternalId, initialRevisionId, 1)))
  }

  it should "create new 'rule live -> tag' join record" in { implicit session =>
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
    RuleTagLive.batchInsert(entities)
    val entitiesAfterBatchInsert = RuleTagLive.findAll()
    entitiesAfterBatchInsert should be(entities)
  }
}
