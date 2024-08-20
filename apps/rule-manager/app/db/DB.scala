package db

import scalikejdbc._

class DB(url: String, user: String, password: String, settings: ConnectionPoolSettings = ConnectionPoolSettings()) {
  Class.forName("org.postgresql.Driver")
  ConnectionPool.singleton(url, user, password, settings)

  def connectionHealthy(): Boolean = {
    val dbString = DB localTx { implicit session =>
      sql""" SELECT 'HELLO WORLD' as hello_world """
        .map { _.string("hello_world") }
        .single()
        .apply()
        .get
    }

    dbString == "HELLO WORLD"
  }

  def closeAll = ConnectionPool.closeAll()
}
