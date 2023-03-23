package db

import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scalikejdbc.scalatest.AutoRollback
import scalikejdbc._
import scala.util.Success

class RulesSpec extends FixtureAnyFlatSpec with Matchers with AutoRollback with DBTest {
  val r = DbRule.syntax("r")

  override def fixture(implicit session: DBSession) {
    sql"ALTER SEQUENCE rules_id_seq RESTART WITH 1".update.apply()
    sql"insert into rules (rule_type, pattern, replacement, category, tags, description, ignore, notes, google_sheet_id, force_red_rule, advisory_rule) values (${"regex"}, ${"pattern"}, ${"replacement"}, ${"category"}, ${"someTags"}, ${"description"}, false, ${"notes"}, ${"googleSheetId"}, false, false)".update.apply()
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
    allResults.size should be >(0)
  }
  it should "count all records" in { implicit session =>
    val count = DbRule.countAll()
    count should be >(0L)
  }
  it should "find all by where clauses" in { implicit session =>
    val results = DbRule.findAllBy(sqls.eq(r.id, 1))
    results.size should be >(0)
  }
  it should "count by where clauses" in { implicit session =>
    val count = DbRule.countBy(sqls.eq(r.id, 1))
    count should be >(0L)
  }
  it should "create new record" in { implicit session =>
    val created = DbRule.create(ruleType = "regex", pattern = "MyString", ignore = false)
    created should not be(null)
  }
  it should "save a record" in { implicit session =>
    val entity = DbRule.findAll().head
    val modified = entity.copy(pattern="NotMyString")
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
    batchInserted.size should be >(0)
  }
}
