package db

import scalikejdbc.scalatest.AutoRollback
import org.scalatest.fixture.FlatSpec
import org.scalatest.matchers.should.Matchers

class HealthyDBSpec extends FlatSpec with Matchers with AutoRollback with DBTest {

  behavior of "Database connection"

  it should "provide a way of testing the DB connection" in { implicit session =>
    scalikejdbcDb.connectionHealthy() shouldBe true
  }
}
