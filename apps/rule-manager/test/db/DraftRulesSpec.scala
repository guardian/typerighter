package db

import model.{CreateRuleForm, UpdateRuleForm}
import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scalikejdbc.scalatest.AutoRollback
import scalikejdbc._
import play.api.mvc.Results.NotFound

import java.time.OffsetDateTime

class DraftRulesSpec extends FixtureAnyFlatSpec with Matchers with AutoRollback with DBTest {

  override def fixture(implicit session: DBSession) = {
    sql"ALTER SEQUENCE rules_id_seq RESTART WITH 1".update().apply()
    sql"insert into rules_draft (rule_type, pattern, replacement, category, tags, description, ignore, notes, external_id, force_red_rule, advisory_rule, created_by, updated_by) values (${"regex"}, ${"pattern"}, ${"replacement"}, ${"category"}, ${"someTags"}, ${"description"}, false, ${"notes"}, ${"externalId"}, false, false, 'test.user', 'test.user')"
      .update()
      .apply()
  }

  def assertDatesAreWithinRangeMs(date1: OffsetDateTime, date2: OffsetDateTime, range: Int) = {
    date1.toInstant().toEpochMilli should be(date2.toInstant().toEpochMilli +- range)
  }

  behavior of "Draft rules"

  it should "find by primary keys and return published status - false" in { implicit session =>
    val found = DbRuleDraft.find(1).get
    found.isPublished should be(false)
  }
  it should "find by primary keys and return published status - true" in { implicit session =>
    val found = DbRuleDraft.find(1).get
    DbRuleLive.create(found.toLive("reason"), "user")
    val foundAndPublished = DbRuleDraft.find(1).get
    foundAndPublished.isPublished should be(true)
  }

  it should "find all records and return published status" in { implicit session =>
    val toBePublished = DbRuleDraft
      .create(ruleType = "regex", pattern = Some("2"), user = "test.user", ignore = false)
      .get
    DbRuleLive.create(toBePublished.toLive("reason"), "user")

    val unpublished :: published :: Nil = DbRuleDraft.findAll()

    unpublished should be(DbRuleDraft.find(1).get)
    unpublished.isPublished should be(false)
    published should be(DbRuleDraft.find(2).get)
    published.isPublished should be(true)
  }

  it should "count all records" in { implicit session =>
    val count = DbRuleDraft.countAll()
    count should be > 0L
  }

  it should "count by where clauses" in { implicit session =>
    val count = DbRuleDraft.countBy(sqls.eq(DbRuleDraft.rd.id, 1))
    count should be > 0L
  }
  it should "create new record and autofill createdAt, updatedAt, and revisionId" in {
    implicit session =>
      val created = DbRuleDraft
        .create(ruleType = "regex", pattern = Some("MyString"), user = "test.user", ignore = false)
        .get

      created.revisionId shouldBe 0
      created.createdBy shouldBe "test.user"
      created.updatedBy shouldBe "test.user"
      assertDatesAreWithinRangeMs(created.createdAt, OffsetDateTime.now, 1000)
      assertDatesAreWithinRangeMs(created.updatedAt, OffsetDateTime.now, 1000)
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
      val existingId = existingRule.id.get
      val formRule = UpdateRuleForm(
        ruleType = Some("regex"),
        pattern = Some("NewString")
      )

      val dbRule =
        DbRuleDraft.updateFromFormRule(formRule, existingId, "another.user").getOrElse(null)

      dbRule.id should be(Some(existingId))
      dbRule.pattern should be(Some("NewString"))
      dbRule.updatedBy should be("another.user")
      dbRule.updatedAt.toInstant.toEpochMilli should be >= existingRule.updatedAt.toInstant.toEpochMilli
  }

  it should "save a record, updating the modified fields and incrementing the revisionId" in {
    implicit session =>
      val entity = DbRuleDraft.findAll().head
      val modified = entity.copy(pattern = Some("NotMyString"))
      val updated = DbRuleDraft.save(modified, "test.user").get
      updated.pattern should equal(Some("NotMyString"))
      updated.updatedBy should equal("test.user")
      updated.updatedAt.toInstant.toEpochMilli should be >= entity.updatedAt.toInstant.toEpochMilli
      updated.revisionId should equal(entity.revisionId + 1)
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
    val batchInserted = DbRuleDraft.batchInsert(entities)
    batchInserted.size should be > (0)
  }

  it should "perform a batch edit on tags and categories" in { implicit session =>
    val existingRule1 = DbRuleDraft
      .create(
        ruleType = "regex",
        category = Some("General"),
        tags = Some("Names,SG,Legal"),
        user = "test.user",
        ignore = false
      )
      .get
    val existingRule2 = DbRuleDraft
      .create(
        ruleType = "regex",
        category = Some("General"),
        tags = Some("Coronavirus"),
        user = "test.user",
        ignore = false
      )
      .get
    val existingRule3 = DbRuleDraft
      .create(
        ruleType = "regex",
        category = Some("General"),
        tags = Some("Typos,Semantics"),
        user = "test.user",
        ignore = false
      )
      .get

    val existingIds = List(existingRule1, existingRule2, existingRule3).map(_.id.get)
    val newCategory = "Style guide and names"
    val newTags = "Names,SG,Legal,Coronavirus,Typos,Semantics"

    val updatedRules =
      DbRuleDraft
        .batchUpdateFromFormRule(existingIds, newCategory, newTags, "another.user")
        .getOrElse(null)

    val updatedRulesFromDb = updatedRules.map { rule =>
      DbRuleDraft.find(rule.id.get)
    }

    updatedRulesFromDb.foreach { maybeRule =>
      maybeRule.foreach { rule =>
        rule.category should be(Some("Style guide and names"))
        rule.tags should be(Some("Names,SG,Legal,Coronavirus,Typos,Semantics"))
      }
    }

    updatedRulesFromDb.size shouldBe 3
  }
}
