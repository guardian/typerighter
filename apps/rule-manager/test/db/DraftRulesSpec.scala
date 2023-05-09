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
  val r = DraftDbRule.syntax("r")

  override def fixture(implicit session: DBSession) = {
    sql"ALTER SEQUENCE rules_id_seq RESTART WITH 1".update().apply()
    sql"insert into rules_draft (rule_type, pattern, replacement, category, tags, description, ignore, notes, google_sheet_id, force_red_rule, advisory_rule, created_by, updated_by) values (${"regex"}, ${"pattern"}, ${"replacement"}, ${"category"}, ${"someTags"}, ${"description"}, false, ${"notes"}, ${"googleSheetId"}, false, false, 'test.user', 'test.user')"
      .update()
      .apply()
  }

  def assertDatesAreWithinRangeMs(date1: ZonedDateTime, date2: ZonedDateTime, range: Int) = {
    date1.toInstant().toEpochMilli should be(date2.toInstant().toEpochMilli +- range)
  }

  behavior of "Draft rules"

  it should "find by primary keys" in { implicit session =>
    val maybeFound = DraftDbRule.find(1)
    maybeFound.isDefined should be(true)
  }
  it should "find by where clauses" in { implicit session =>
    val maybeFound = DraftDbRule.findBy(sqls.eq(r.id, 1))
    maybeFound.isDefined should be(true)
  }
  it should "find all records" in { implicit session =>
    val allResults = DraftDbRule.findAll()
    allResults.size should be > (0)
  }
  it should "count all records" in { implicit session =>
    val count = DraftDbRule.countAll()
    count should be > (0L)
  }
  it should "find all by where clauses" in { implicit session =>
    val results = DraftDbRule.findAllBy(sqls.eq(r.id, 1))
    results.size should be > (0)
  }
  it should "count by where clauses" in { implicit session =>
    val count = DraftDbRule.countBy(sqls.eq(r.id, 1))
    count should be > (0L)
  }
  it should "create new record and autofill createdAt, updatedAt, and revisionId" in {
    implicit session =>
      val created = DraftDbRule
        .create(ruleType = "regex", pattern = Some("MyString"), user = "test.user", ignore = false)
        .get

      created.revisionId shouldBe 0
      created.createdBy shouldBe "test.user"
      created.updatedBy shouldBe "test.user"
      assertDatesAreWithinRangeMs(created.createdAt, ZonedDateTime.now, 1000)
      assertDatesAreWithinRangeMs(created.updatedAt, ZonedDateTime.now, 1000)
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
      googleSheetId = None,
      forceRedRule = None,
      advisoryRule = None
    )
    val dbRule = DraftDbRule.createFromFormRule(formRule, user = "test.user")
    dbRule should not be (null)
  }

  it should "edit an existing record using a form rule, updating the user and updated datetime" in {
    implicit session =>
      val existingRule = DraftDbRule
        .create(ruleType = "regex", pattern = Some("MyString"), user = "test.user", ignore = false)
        .get
      val existingId = existingRule.id.get
      val formRule = UpdateRuleForm(
        ruleType = Some("regex"),
        pattern = Some("NewString")
      )

      val dbRule =
        DraftDbRule.updateFromFormRule(formRule, existingId, "another.user").getOrElse(null)

      dbRule.id should be(Some(existingId))
      dbRule.pattern should be(Some("NewString"))
      dbRule.updatedBy should be("another.user")
      dbRule.updatedAt.toInstant.toEpochMilli should be > existingRule.updatedAt.toInstant.toEpochMilli
  }

  it should "save a record, updating the modified fields and incrementing the revisionId" in {
    implicit session =>
      val entity = DraftDbRule.findAll().head
      val modified = entity.copy(pattern = Some("NotMyString"))
      val updated = DraftDbRule.save(modified, "test.user").get
      updated.pattern should equal(Some("NotMyString"))
      updated.updatedBy should equal("test.user")
      updated.updatedAt.toInstant.toEpochMilli should be > entity.updatedAt.toInstant.toEpochMilli
      updated.revisionId should equal(entity.revisionId + 1)
  }

  it should "return an error when attempting to update a record that doesn't exist" in {
    implicit session =>
      val formRule = UpdateRuleForm(
        ruleType = Some("regex"),
        pattern = Some("NewString")
      )
      val nonExistentRuleId = 2000
      val dbRule = DraftDbRule.updateFromFormRule(formRule, nonExistentRuleId, "test.user")
      dbRule should be(Left(NotFound("Rule not found matching ID")))
  }

  it should "destroy a record" in { implicit session =>
    val entity = DraftDbRule.findAll().head
    val deleted = DraftDbRule.destroy(entity)
    deleted should be(1)
    val shouldBeNone = DraftDbRule.find(123)
    shouldBeNone.isDefined should be(false)
  }

  it should "perform batch insert" in { implicit session =>
    val entities = DraftDbRule.findAll()
    entities.foreach(e => DraftDbRule.destroy(e))
    val batchInserted = DraftDbRule.batchInsert(entities)
    batchInserted.size should be > (0)
  }
}
