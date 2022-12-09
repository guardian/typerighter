package db

import scalikejdbc.scalatest.AutoRollback
import org.scalatest.flatspec.FixtureAnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class RuleManagerDBTest
  extends FixtureAnyFlatSpecLike
    with AutoRollback
    with Matchers {
  val url ="jdbc:postgresql://localhost:5432/tr-rule-manager-local"
  val username ="tr-rule-manager-local"
  val password ="tr-rule-manager-local"
  val ruleManagerDb = new RuleManagerDB(url, username, password)

  behavior of "Database connection"

  it should "provide a way of testing the DB connection" in { () =>
    ruleManagerDb.connectionHealthy() shouldBe true
  }
}
