package db

import org.scalatest.{BeforeAndAfter, Suite}
import play.api.db.Databases
import play.api.db.evolutions.{Evolutions, InconsistentDatabase}

trait RuleManagerDBTest extends BeforeAndAfter { self: Suite =>
  val url = "jdbc:postgresql://localhost:5432/tr-rule-manager-local"
  val user = "tr-rule-manager-local"
  val password = "tr-rule-manager-local"
  val scalikejdbcDb = new RuleManagerDB(url, user, password)
  var playDb = Databases("org.postgresql.Driver", url, config = Map(
    "username" -> user,
    "password" -> password
  ))

  before {
    try {
      Evolutions.applyEvolutions(playDb)
    } catch {
      case fail: InconsistentDatabase =>
        println(fail.subTitle)
        throw fail
    }
  }

  after {
    Evolutions.cleanupEvolutions(playDb)
  }
}
