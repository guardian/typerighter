package db

import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scalikejdbc._
import scalikejdbc.scalatest.AutoRollback

class LiveRulesSpec extends FixtureAnyFlatSpec with Matchers with AutoRollback with DBTest {
  val r = DbRuleLive.syntax("r")

  override def fixture(implicit session: DBSession) = {
    sql"ALTER SEQUENCE rules_id_seq RESTART WITH 1".update().apply()
    sql"insert into rules_live (rule_type, pattern, replacement, category, tags, description, notes, external_id, force_red_rule, advisory_rule, created_by, updated_by, rule_order) values (${"regex"}, ${"pattern"}, ${"replacement"}, ${"category"}, ${"someTags"}, ${"description"}, ${"notes"}, ${"googleSheetId"}, false, false, 'test.user', 'test.user', 1)"
      .update()
      .apply()
  }

  behavior of "Live rules"

  it should "find by primary keys" in { implicit session =>
    val maybeFound = DbRuleLive.find("googleSheetId", 0)
    maybeFound.isDefined should be(true)
  }

  it should "find all records" in { implicit session =>
    val maybeFound = DbRuleLive.find("googleSheetId", 0)
    val allResults = DbRuleLive.findAll()
    allResults should be(List(maybeFound.get))
  }

  it should "perform batch insert" in { implicit session =>
    val entities = DbRuleLive.findAll()
    DbRuleLive.destroyAll()
    val batchInserted = DbRuleLive.batchInsert(entities)
    batchInserted.size should be > (0)
  }
}
