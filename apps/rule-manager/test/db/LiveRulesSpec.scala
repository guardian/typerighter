package db

import org.scalatest.matchers.should.Matchers

class LiveRulesSpec extends RuleFixture with Matchers with DBTest {
  val r = DbRuleLive.syntax("r")

  behavior of "Live rules"

  it should "find by primary keys" in { implicit session =>
    val maybeFound = DbRuleLive.findRevision("googleSheetId", 0)
    maybeFound.isDefined should be(true)
  }

  it should "find all records" in { implicit session =>
    val maybeFound = DbRuleLive.findRevision("googleSheetId", 0)
    val allResults = DbRuleLive.findAll()
    allResults should be(List(maybeFound.get))
  }

  it should "create a new rule" in { implicit session =>
    val dbRuleLive = DbRuleLive.findRevision("googleSheetId", 0).get
    val newRule = dbRuleLive.copy(revisionId = dbRuleLive.revisionId + 1)
    val savedRule = DbRuleLive.create(newRule, "test.user").get
    savedRule shouldBe newRule
  }

  it should "perform batch insert" in { implicit session =>
    val entities = DbRuleLive.findAll()
    RuleTagLive.destroyAll()
    DbRuleLive.destroyAll()
    DbRuleLive.batchInsert(entities)
    val batchInserted = DbRuleLive.findAll()
    batchInserted shouldBe entities
  }
}
