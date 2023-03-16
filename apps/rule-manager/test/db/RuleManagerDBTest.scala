package db

import org.scalatest.{BeforeAndAfter, Suite}
import play.api.{ConfigLoader, Configuration, Environment}
import play.api.db.Databases
import play.api.db.evolutions.{Evolutions, InconsistentDatabase}

trait RuleManagerDBTest extends BeforeAndAfter { self: Suite =>
  private implicit val loader = ConfigLoader.stringLoader
  private val config = Configuration.load(Environment.simple(), Map.empty)

  private val url = config.get("db.default.url")
  private val user = config.get("db.default.username")
  private val password = config.get("db.default.password")
  private val playDb = Databases("org.postgresql.Driver", url, config = Map(
    "username" -> user,
    "password" -> password
  ))

  val scalikejdbcDb = new DB(url, user, password)

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
