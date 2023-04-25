package db

import org.scalatest.flatspec.FixtureAnyFlatSpec
import scalikejdbc.scalatest.AutoRollback
import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.FixtureAnyFlatSpec

class HealthyDBSpec extends FixtureAnyFlatSpec with Matchers with AutoRollback with DBTest {

  behavior of "Database connection"

  it should "provide a way of testing the DB connection" in { implicit session =>
    scalikejdbcDb.connectionHealthy() shouldBe true
  }
}
