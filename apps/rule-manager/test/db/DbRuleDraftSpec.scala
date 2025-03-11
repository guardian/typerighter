package db

import model.{CreateRuleForm, UpdateRuleForm}
import org.scalatest.matchers.should.Matchers
import com.softwaremill.diffx.generic.auto.diffForCaseClass
import com.softwaremill.diffx.scalatest.DiffShouldMatcher._
import scalikejdbc._
import play.api.mvc.Results.NotFound

import java.time.OffsetDateTime
import service.RuleManager.RuleType

class DbRuleDraftSpec extends RuleFixture with Matchers with DBTest {

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

  it should "find by primary keys and return unpublished changes status - false, unpublished" in {
    implicit session =>
      val found = DbRuleDraft.find(1).get
      found.hasUnpublishedChanges should be(false)
  }

  it should "find by primary keys and return unpublished changes status - false, published" in {
    implicit session =>
      val found = DbRuleDraft.find(1).get
      DbRuleLive.create(found.toLive("reason"), "user")
      val foundAndPublished = DbRuleDraft.find(1).get
      foundAndPublished.hasUnpublishedChanges should be(false)
  }

  it should "find by primary keys and return unpublished changes status - true" in {
    implicit session =>
      val found = DbRuleDraft.find(1).get
      DbRuleLive.create(found.toLive("reason"), "user")
      val foundAndPublished =
        DbRuleDraft.save(found.copy(description = Some("updated")), "test.user").get
      foundAndPublished.hasUnpublishedChanges should be(true)
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

  it should "sort by updated_at if no sort order is specified" in { implicit session =>
    val rule1 = DbRuleDraft.find(1).get
    val rule2 = insertRule(ruleType = "dictionary")
    val rule3 = insertRule(ruleType = "dictionary")

    val formRule = UpdateRuleForm(
      pattern = Some("New pattern"),
      tags = List.empty
    )

    val rule3After =
      DbRuleDraft.updateFromFormRule(formRule, rule3.id.get, "another.user").getOrElse(null)

    val results = DbRuleDraft.searchRules(1)
    results.data.map(_.pattern) shouldBe List(rule3After, rule2, rule1).map(_.pattern)
  }

  it should "return pagination results when searching" in { implicit session =>
    val totalNoOfRules = 26
    val pageSize = 10
    ('a' to 'z').foreach { ruleNo =>
      DbRuleDraft
        .create(
          ruleType = "regex",
          pattern = Some(s"Rule $ruleNo"),
          user = "test.user",
          ignore = false
        )
        .get
    }

    val results = DbRuleDraft.searchRules(
      page = 1,
      maybeWord = Some("Rule"),
      sortBy = List("+pattern"),
      pageSize = pageSize
    )
    results.data.size shouldBe pageSize
    results.data.flatMap(_.pattern) shouldBe ('a' to 'j').map(n => s"Rule $n").toList
    results.total shouldBe totalNoOfRules
    results.page shouldBe 1
    results.pageSize shouldBe pageSize
  }

  it should "return correct pagination results when filtering by ruleType" in { implicit session =>
    val totalNoOfRules = 26
    val pageSize = 10
    ('a' to 'z').map { ruleNo =>
      DbRuleDraft
        .create(
          ruleType = if (ruleNo % 2 == 0) "regex" else "dictionary",
          pattern = Some(s"Rule $ruleNo"),
          user = "test.user",
          ignore = false
        )
        .get
    }

    val results = DbRuleDraft.searchRules(
      page = 1,
      ruleTypes = List("dictionary"),
      pageSize = pageSize
    )

    results.data.size shouldBe pageSize
    results.total shouldBe totalNoOfRules / 2
    results.page shouldBe 1
    results.pageSize shouldBe pageSize
  }

  it should "return correct pagination results when filtering by tag" in { implicit session =>
    val totalNoOfRules = 26
    val pageSize = 10
    ('a' to 'z').map { ruleNo =>
      DbRuleDraft
        .create(
          tags = if (ruleNo % 2 == 0) List(1) else List.empty,
          ruleType = "regex",
          pattern = Some(s"Rule $ruleNo"),
          user = "test.user",
          ignore = false
        )
        .get
    }

    val results = DbRuleDraft.searchRules(
      page = 1,
      tags = List(1),
      pageSize = pageSize
    )

    results.data.size shouldBe pageSize
    results.total shouldBe (totalNoOfRules / 2) + 1 // Plus initial rule fixture, which has a tag
    results.page shouldBe 1
    results.pageSize shouldBe pageSize
  }

  it should "return subsequent pages correctly" in { implicit session =>
    val totalNoOfRules = 26
    val pageSize = 10
    ('a' to 'z').foreach { ruleNo =>
      DbRuleDraft
        .create(
          ruleType = "regex",
          pattern = Some(s"Rule $ruleNo"),
          user = "test.user",
          ignore = false
        )
        .get
    }

    val results = DbRuleDraft.searchRules(
      page = 2,
      maybeWord = Some("Rule"),
      sortBy = List("+pattern"),
      pageSize = pageSize
    )
    results.data.size shouldBe pageSize
    results.data.flatMap(_.pattern) shouldBe ('k' to 't').map(n => s"Rule $n")
    results.total shouldBe totalNoOfRules
    results.page shouldBe 2
    results.pageSize shouldBe pageSize
  }

  it should "search rules using a partial search phrase – pattern" in { implicit session =>
    val rule = DbRuleDraft
      .create(
        ruleType = "regex",
        pattern = Some("The cat sat on the mat"),
        user = "test.user",
        ignore = false
      )
      .get

    val results = DbRuleDraft.searchRules(1, Some("The ca"))
    results.data shouldBe List(rule)
  }

  it should "search rules using a partial search phrase – description" in { implicit session =>
    val rule = DbRuleDraft
      .create(
        ruleType = "regex",
        description = Some("The cat sat on the mat"),
        user = "test.user",
        ignore = false
      )
      .get

    val results = DbRuleDraft.searchRules(1, Some("The ca"))
    results.data shouldBe List(rule)
  }

  it should "search rules using a partial search phrase – replacement" in { implicit session =>
    val rule = DbRuleDraft
      .create(
        ruleType = "regex",
        replacement = Some("The cat sat on the mat"),
        user = "test.user",
        ignore = false
      )
      .get

    val results = DbRuleDraft.searchRules(1, Some("The ca"))
    results.data shouldBe List(rule)
  }

  it should "prioritise the exact match when ordering results" in { implicit session =>
    val rule1 = insertRule(pattern = Some("Example"))
    val rule2 = insertRule(pattern = Some("An example rule"))
    val rule3 = insertRule(pattern = Some("Another example rule"))

    val results = DbRuleDraft.searchRules(1, Some("Example"))
    results.data.map(_.pattern) shouldBe List(rule1, rule2, rule3).map(_.pattern)
  }

  it should "search by tag" in { implicit session =>
    Tags.create("Tag 2")
    Tags.create("Tag 3")

    val rule1 = DbRuleDraft.find(1).get
    insertRule(tags = List(2))
    insertRule(tags = List(3))

    val results = DbRuleDraft.searchRules(1, tags = List(1))
    results.data.map(_.pattern) shouldBe List(rule1).map(_.pattern)
  }

  it should "search by type" in { implicit session =>
    val rule1 = DbRuleDraft.find(1).get
    insertRule(ruleType = "dictionary")
    insertRule(ruleType = "dictionary")

    val results = DbRuleDraft.searchRules(1, ruleTypes = List("regex"))
    results.data.map(_.pattern) shouldBe List(rule1).map(_.pattern)
  }

  it should "combine query string, tag, and type searches" in { implicit session =>
    Tags.create("Tag 2")
    val rule = insertRule(ruleType = "dictionary", pattern = Some("findme"), tags = List(2))

    val results = DbRuleDraft.searchRules(
      1,
      ruleTypes = List("dictionary"),
      maybeWord = Some("findme"),
      tags = List(2)
    )
    results.data.map(_.pattern) shouldBe List(rule).map(_.pattern)
  }

  it should "order rules given a sort column, ASC and DESC" in { implicit session =>
    DbRuleDraft
      .create(
        ruleType = "regex",
        replacement = Some("A"),
        user = "test.user",
        ignore = false
      )
    DbRuleDraft
      .create(
        ruleType = "regex",
        replacement = Some("B"),
        user = "test.user",
        ignore = false
      )

    val descResults = DbRuleDraft.searchRules(page = 1, sortBy = List("-replacement"))
    descResults.data shouldMatchTo descResults.data.sortBy(_.replacement.getOrElse("")).reverse

    val ascResults = DbRuleDraft.searchRules(page = 1, sortBy = List("+replacement"))
    ascResults.data shouldMatchTo ascResults.data.sortBy(_.replacement.getOrElse(""))
  }

  it should "order rules given multiple sort columns, ASC and DESC" in { implicit session =>
    val ruleA = DbRuleDraft.findAll().head
    val ruleB = DbRuleDraft
      .create(
        ruleType = "regex",
        description = Some("B"),
        replacement = Some("Same replacement"),
        user = "test.user",
        ignore = false
      )
      .get
    val ruleC = DbRuleDraft
      .create(
        ruleType = "regex",
        description = Some("A"),
        replacement = Some("Same replacement"),
        user = "test.user",
        ignore = false
      )
      .get

    val descResults = DbRuleDraft
      .searchRules(page = 1, sortBy = List("-description", "-replacement"))
      .data
      .map(d => d.description -> d.replacement)
    val descExpected = List(
      ruleA,
      ruleB,
      ruleC
    ).map(d => d.description -> d.replacement)

    descResults shouldMatchTo descExpected

    val ascResults = DbRuleDraft
      .searchRules(page = 1, None, sortBy = List("+description", "+replacement"))
      .data
      .map(d => d.description -> d.replacement)
    val ascExpected = List(ruleC, ruleB, ruleA)
      .map(d => d.description -> d.replacement)

    ascResults shouldMatchTo ascExpected
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

  it should s"store external IDs when creating rules of type ${RuleType.languageToolCore}" in {
    implicit session =>
      val formRule = CreateRuleForm(
        ruleType = "languageToolCore",
        pattern = None,
        replacement = None,
        category = None,
        tags = None,
        description = None,
        ignore = false,
        notes = None,
        forceRedRule = None,
        advisoryRule = None,
        externalId = Some("EXTERNAL_ID")
      )
      val dbRule = DbRuleDraft.createFromFormRule(formRule, user = "test.user")
      dbRule.toOption.get.externalId shouldBe Some("EXTERNAL_ID")
  }

  it should s"generate a friendly placeholder for external ID when creating rules of type ${RuleType.languageToolCore}" in {
    implicit session =>
      val formRule = CreateRuleForm(
        ruleType = "languageToolCore",
        pattern = None,
        replacement = None,
        category = None,
        tags = None,
        description = None,
        ignore = false,
        notes = None,
        forceRedRule = None,
        advisoryRule = None,
        externalId = None
      )
      val dbRule = DbRuleDraft.createFromFormRule(formRule, user = "test.user")
      dbRule.toOption.get.externalId shouldBe Some("CHANGE_ME")
  }

  it should "edit an existing record using a form rule, updating the user and updated datetime" in {
    implicit session =>
      val existingRule = DbRuleDraft
        .create(ruleType = "regex", pattern = Some("MyString"), user = "test.user", ignore = false)
        .get
      val existingId = existingRule.id.get
      val formRule = UpdateRuleForm(
        ruleType = Some("regex"),
        pattern = Some("NewString"),
        tags = List.empty
      )

      val dbRule =
        DbRuleDraft.updateFromFormRule(formRule, existingId, "another.user").getOrElse(null)

      dbRule.id should be(Some(existingId))
      dbRule.pattern should be(Some("NewString"))
      dbRule.updatedBy should be("another.user")
      dbRule.updatedAt.toInstant.toEpochMilli should be >= existingRule.updatedAt.toInstant.toEpochMilli
  }

  it should s"update external IDs for rules of type ${RuleType.languageToolCore}" in {
    implicit session =>
      val existingRule = DbRuleDraft
        .create(
          ruleType = "languageToolCore",
          externalId = Some("EXTERNAL_ID"),
          user = "test.user",
          ignore = false
        )
        .get
      val existingId = existingRule.id.get
      val formRule = UpdateRuleForm(
        ruleType = Some("languageToolCore"),
        externalId = Some("EXTERNAL_ID_UPDATE"),
        tags = List.empty
      )

      val dbRule =
        DbRuleDraft.updateFromFormRule(formRule, existingId, "another.user").getOrElse(null)

      dbRule.id should be(Some(existingId))
      dbRule.externalId should be(Some("EXTERNAL_ID_UPDATE"))
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
        pattern = Some("NewString"),
        tags = List.empty
      )
      val nonExistentRuleId = 2000
      val dbRule = DbRuleDraft.updateFromFormRule(formRule, nonExistentRuleId, "test.user")
      dbRule should be(Left(NotFound("Rule not found matching ID")))
  }

  it should "destroy a record" in { implicit session =>
    val entity = DbRuleDraft.findAll().head
    RuleTagDraft.destroyAll() // Necessary to remove fk
    val deleted = DbRuleDraft.destroy(entity)
    deleted should be(1)
    val shouldBeNone = DbRuleDraft.find(123)
    shouldBeNone.isDefined should be(false)
  }

  it should "perform batch insert" in { implicit session =>
    val entities = DbRuleDraft.findAll()
    RuleTagDraft.findAll().map(ruleTag => RuleTagDraft.destroy(ruleTag))
    entities.foreach(e => DbRuleDraft.destroy(e))
    DbRuleDraft.batchInsert(entities)

    val insertedRules = DbRuleDraft.findAll()
    val indexOffset = entities.size // The
    val insertedRuleWithNormalisedIds = insertedRules.zipWithIndex.map { case (rule, index) =>
      rule.copy(id = Some(index + indexOffset))
    }
    insertedRuleWithNormalisedIds shouldMatchTo entities
  }

  it should "perform a batch edit on tags and categories" in { implicit session =>
    Tags.create("Another tag")
    val tags = Tags.findAll()
    val existingRule1 = DbRuleDraft
      .create(
        ruleType = "regex",
        category = Some("General"),
        tags = List(tags.head.id.get),
        user = "test.user",
        ignore = false
      )
      .get
    val existingRule2 = DbRuleDraft
      .create(
        ruleType = "regex",
        category = Some("General"),
        tags = List(tags.head.id.get, tags(1).id.get),
        user = "test.user",
        ignore = false
      )
      .get
    val existingRule3 = DbRuleDraft
      .create(
        ruleType = "regex",
        category = Some("General"),
        tags = List.empty,
        user = "test.user",
        ignore = false
      )
      .get

    val existingIds = List(existingRule1, existingRule2, existingRule3).map(_.id.get)
    val newCategory = Some("Style guide and names")
    val newTags = tags.map(_.id.get)

    DbRuleDraft
      .batchUpdate(existingIds, newCategory, Some(newTags), "another.user")

    val updatedRulesFromDb = DbRuleDraft.findRules(existingIds)

    updatedRulesFromDb.foreach { maybeRule =>
      maybeRule.category should be(Some("Style guide and names"))
      maybeRule.tags should be(newTags)
    }

    updatedRulesFromDb.size shouldBe 3
  }
}
