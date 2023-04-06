package db

import model.CreateRuleForm
import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scalikejdbc.scalatest.AutoRollback
import scalikejdbc._
import play.api.libs.json.{JsValue, Json}


import scala.util.Success

class RulesSpec extends FixtureAnyFlatSpec with Matchers with AutoRollback with DBTest {
  val r = DbRule.syntax("r")

  override def fixture(implicit session: DBSession) {
    sql"ALTER SEQUENCE rules_id_seq RESTART WITH 1".update.apply()
    sql"insert into rules (rule_type, pattern, replacement, category, tags, description, ignore, notes, google_sheet_id, force_red_rule, advisory_rule) values (${"regex"}, ${"pattern"}, ${"replacement"}, ${"category"}, ${"someTags"}, ${"description"}, false, ${"notes"}, ${"googleSheetId"}, false, false)".update
      .apply()
  }

  behavior of "Rules"

  it should "find by primary keys" in { implicit session =>
    val maybeFound = DbRule.find(1)
    maybeFound.isDefined should be(true)
  }
  it should "find by where clauses" in { implicit session =>
    val maybeFound = DbRule.findBy(sqls.eq(r.id, 1))
    maybeFound.isDefined should be(true)
  }
  it should "find all records" in { implicit session =>
    val allResults = DbRule.findAll()
    allResults.size should be > (0)
  }
  it should "count all records" in { implicit session =>
    val count = DbRule.countAll()
    count should be > (0L)
  }
  it should "find all by where clauses" in { implicit session =>
    val results = DbRule.findAllBy(sqls.eq(r.id, 1))
    results.size should be > (0)
  }
  it should "count by where clauses" in { implicit session =>
    val count = DbRule.countBy(sqls.eq(r.id, 1))
    count should be > (0L)
  }
  it should "create new record" in { implicit session =>
    val created = DbRule.create(ruleType = "regex", pattern = Some("MyString"), ignore = false)
    created should not be (null)
  }
  // TODO: Figure out why this test won't run
//  it should "create a new record from a form rule" in { implicit session =>
//   val formRule = CreateRuleForm(
//      ruleType = "regex",
//      pattern = None,
//      replacement = None,
//      category = None,
//      tags = None,
//      description = None,
//      ignore = false,
//      notes = None,
//      googleSheetId = None,
//      forceRedRule = None,
//      advisoryRule = None
//    )
//    val dbRule = DbRule.createFromFormRule(formRule)
//    dbRule should not be (null)
//  }
  it should "save a record" in { implicit session =>
    val entity = DbRule.findAll().head
    val modified = entity.copy(pattern = Some("NotMyString"))
    val updated = DbRule.save(modified)
    updated should equal(Success(modified))
  }
  it should "destroy a record" in { implicit session =>
    val entity = DbRule.findAll().head
    val deleted = DbRule.destroy(entity)
    deleted should be(1)
    val shouldBeNone = DbRule.find(123)
    shouldBeNone.isDefined should be(false)
  }
  it should "perform batch insert" in { implicit session =>
    val entities = DbRule.findAll()
    entities.foreach(e => DbRule.destroy(e))
    val batchInserted = DbRule.batchInsert(entities)
    batchInserted.size should be > (0)
  }
  it should "return a JSON representation of a rule" in { implicit session =>
    val created = DbRule.create(ruleType = "regex", pattern = Some("MyString"), ignore = false)
    val json = DbRule.toJson(created)
    val expected = Json.parse(s"""
    {
      "ruleType" : "regex",
      "forceRedRule": null,
      "replacement": null,
      "advisoryRule": null,
      "id": ${created.id.get},
      "category": null,
      "notes": null,
      "ignore": false,
      "pattern": "MyString",
      "googleSheetId": null,
      "description": null,
      "tags": null
    }
    """)
    json should equal(expected)
  }
}
