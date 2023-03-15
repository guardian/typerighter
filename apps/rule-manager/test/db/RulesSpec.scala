package db

import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scalikejdbc.scalatest.AutoRollback
import scalikejdbc._

class RulesSpec extends FixtureAnyFlatSpec with Matchers with AutoRollback with RuleManagerDBTest {
  val r = Rules.syntax("r")

  override def fixture(implicit session: DBSession) {
    sql"ALTER SEQUENCE rules_id_seq RESTART WITH 1".update.apply()
    sql"insert into rules (rule_type, pattern, replacement, category, tags, description, ignore, notes, google_sheet_id, force_red_rule, advisory_rule) values (${"regex"}, ${"pattern"}, ${"replacement"}, ${"category"}, ${"someTags"}, ${"description"}, false, ${"notes"}, ${"googleSheetId"}, false, false)".update.apply()
  }

  behavior of "Rules"

  it should "find by primary keys" in { implicit session =>
    val maybeFound = Rules.find(1)
    maybeFound.isDefined should be(true)
  }
  it should "find by where clauses" in { implicit session =>
    val maybeFound = Rules.findBy(sqls.eq(r.id, 1))
    maybeFound.isDefined should be(true)
  }
  it should "find all records" in { implicit session =>
    val allResults = Rules.findAll()
    allResults.size should be >(0)
  }
  it should "count all records" in { implicit session =>
    val count = Rules.countAll()
    count should be >(0L)
  }
  it should "find all by where clauses" in { implicit session =>
    val results = Rules.findAllBy(sqls.eq(r.id, 1))
    results.size should be >(0)
  }
  it should "count by where clauses" in { implicit session =>
    val count = Rules.countBy(sqls.eq(r.id, 1))
    count should be >(0L)
  }
  it should "create new record" in { implicit session =>
    val created = Rules.create(ruleType = "regex", pattern = "MyString", ignore = false)
    created should not be(null)
  }
  it should "save a record" in { implicit session =>
    val entity = Rules.findAll().head
    // TODO modify something
    val modified = entity.copy(pattern="NotMyString")
    val updated = Rules.save(modified)
    updated should equal(modified)
  }
  it should "destroy a record" in { implicit session =>
    val entity = Rules.findAll().head
    val deleted = Rules.destroy(entity)
    deleted should be(1)
    val shouldBeNone = Rules.find(123)
    shouldBeNone.isDefined should be(false)
  }
  it should "perform batch insert" in { implicit session =>
    val entities = Rules.findAll()
    entities.foreach(e => Rules.destroy(e))
    val batchInserted = Rules.batchInsert(entities)
    batchInserted.size should be >(0)
  }
}
