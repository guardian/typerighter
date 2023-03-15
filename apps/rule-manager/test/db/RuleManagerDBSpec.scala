package db

import scalikejdbc.scalatest.AutoRollback
import org.scalatest.fixture.FlatSpec
import org.scalatest.matchers.should.Matchers

class RuleManagerDBSpec
  extends FlatSpec
  with Matchers
  with AutoRollback
  with RuleManagerDBTest {

  behavior of "Database connection"

  it should "provide a way of testing the DB connection" in { implicit session =>
    scalikejdbcDb.connectionHealthy() shouldBe true
  }
}
