package db

import model.CreateRuleForm
import model.UpdateRuleForm
import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scalikejdbc.scalatest.AutoRollback
import scalikejdbc._
import play.api.mvc.Results.NotFound

import java.time.ZonedDateTime

class DraftRulesSpec extends FixtureAnyFlatSpec with Matchers with AutoRollback with DBTest {
  val r = DbRuleDraft.syntax("r")

  override def fixture(implicit session: DBSession) = {
    sql"ALTER SEQUENCE rules_id_seq RESTART WITH 1".update().apply()
    sql"insert into rules_draft (rule_type, pattern, replacement, category, tags, description, ignore, notes, external_id, force_red_rule, advisory_rule, created_by, updated_by) values (${"regex"}, ${"pattern"}, ${"replacement"}, ${"category"}, ${"someTags"}, ${"description"}, false, ${"notes"}, ${"externalId"}, false, false, 'test.user', 'test.user')"
      .update()
      .apply()
  }

  def assertDatesAreWithinRangeMs(date1: ZonedDateTime, date2: ZonedDateTime, range: Int) = {
    date1.toInstant().toEpochMilli should be(date2.toInstant().toEpochMilli +- range)
  }

  behavior of "Draft rules"

  it should "find by primary keys" in { implicit session =>
    val maybeFound = DbRuleDraft.find(1)
    maybeFound.isDefined should be(true)
  }
  it should "find by where clauses" in { implicit session =>
    val maybeFound = DbRuleDraft.findBy(sqls.eq(r.id, 1))
    maybeFound.isDefined should be(true)
  }
  it should "find all records" in { implicit session =>
    val allResults = DbRuleDraft.findAll()
    allResults.size should be > (0)
  }
  it should "count all records" in { implicit session =>
    val count = DbRuleDraft.countAll()
    count should be > (0L)
  }
  it should "find all by where clauses" in { implicit session =>
    val results = DbRuleDraft.findAllBy(sqls.eq(r.id, 1))
    results.size should be > (0)
  }
  it should "count by where clauses" in { implicit session =>
    val count = DbRuleDraft.countBy(sqls.eq(r.id, 1))
    count should be > (0L)
  }
  it should "create new record and autofill createdAt, updatedAt, and revisionId" in {
    implicit session =>
      val created = DbRuleDraft
        .create(ruleType = "regex", pattern = Some("MyString"), user = "test.user", ignore = false)
        .get

      created.entity.revisionId shouldBe 0
      created.entity.createdBy shouldBe "test.user"
      created.entity.updatedBy shouldBe "test.user"
      assertDatesAreWithinRangeMs(created.entity.createdAt, ZonedDateTime.now, 1000)
      assertDatesAreWithinRangeMs(created.entity.updatedAt, ZonedDateTime.now, 1000)
  }
  it should "create a new record from a form rule" in { implicit session =>
    val formRule = CreateRuleForm(
      ruleType = "regex",
      pattern = None,
      replacement = None,
      category = None,
      tags = None,
      description = None,
      ignore = false,
      notes = None,
      forceRedRule = None,
      advisoryRule = None
    )
    val dbRule = DbRuleDraft.createFromFormRule(formRule, user = "test.user")
    dbRule should not be (null)
  }

  it should "edit an existing record using a form rule, updating the user and updated datetime" in {
    implicit session =>
      val existingRule = DbRuleDraft
        .create(ruleType = "regex", pattern = Some("MyString"), user = "test.user", ignore = false)
        .get
      val existingId = existingRule.id
      val formRule = UpdateRuleForm(
        ruleType = Some("regex"),
        pattern = Some("NewString")
      )

      val dbRule =
        DbRuleDraft.updateFromFormRule(formRule, existingId, "another.user").getOrElse(null)

      dbRule.id should be(Some(existingId))
      dbRule.entity.pattern should be(Some("NewString"))
      dbRule.entity.updatedBy should be("another.user")
      dbRule.entity.updatedAt.toInstant.toEpochMilli should be > existingRule.entity.updatedAt.toInstant.toEpochMilli
  }

  it should "save a record, updating the modified fields and incrementing the revisionId" in {
    implicit session =>
      val entity = DbRuleDraft.findAll().head
      val modified = WithId(entity.entity.copy(pattern = Some("NotMyString")), entity.id)
      val updated = DbRuleDraft.save(modified, "test.user").get
      updated.entity.pattern should equal(Some("NotMyString"))
      updated.entity.updatedBy should equal("test.user")
      updated.entity.updatedAt.toInstant.toEpochMilli should be >= entity.entity.updatedAt.toInstant.toEpochMilli
      updated.entity.revisionId should equal(entity.entity.revisionId + 1)
  }

  it should "return an error when attempting to update a record that doesn't exist" in {
    implicit session =>
      val formRule = UpdateRuleForm(
        ruleType = Some("regex"),
        pattern = Some("NewString")
      )
      val nonExistentRuleId = 2000
      val dbRule = DbRuleDraft.updateFromFormRule(formRule, nonExistentRuleId, "test.user")
      dbRule should be(Left(NotFound("Rule not found matching ID")))
  }

  it should "destroy a record" in { implicit session =>
    val entity = DbRuleDraft.findAll().head
    val deleted = DbRuleDraft.destroy(entity)
    deleted should be(1)
    val shouldBeNone = DbRuleDraft.find(123)
    shouldBeNone.isDefined should be(false)
  }

  it should "perform batch insert" in { implicit session =>
    val entities = DbRuleDraft.findAll()
    entities.foreach(e => DbRuleDraft.destroy(e))
    val batchInserted = DbRuleDraft.batchInsert(entities.map(_.entity))
    batchInserted.size should be > (0)
  }
}
