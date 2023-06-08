package db

package db

import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scalikejdbc.scalatest.AutoRollback
import scalikejdbc._

class TagsSpec extends FixtureAnyFlatSpec with Matchers with AutoRollback with DBTest {
  val t = Tags.syntax("t")

  override def fixture(implicit session: DBSession) = {
    sql"ALTER SEQUENCE tags_id_seq RESTART WITH 1".update().apply()
    sql"insert into tags (name) values (${"name"})"
      .update()
      .apply()
  }

  behavior of "Tags"

  it should "find by primary keys" in { implicit session =>
    val maybeFound = Tags.find(1)
    maybeFound.isDefined should be(true)
  }
  it should "find by where clauses" in { implicit session =>
    val maybeFound = Tags.findBy(sqls.eq(t.id, 1))
    maybeFound.isDefined should be(true)
  }
  it should "find all records" in { implicit session =>
    val allResults = Tags.findAll()
    allResults.size should be > (0)
  }
  it should "count all records" in { implicit session =>
    val count = Tags.countAll()
    count should be > (0L)
  }
  it should "find all by where clauses" in { implicit session =>
    val results = Tags.findAllBy(sqls.eq(t.id, 1))
    results.size should be > (0)
  }
  it should "count by where clauses" in { implicit session =>
    val count = Tags.countBy(sqls.eq(t.id, 1))
    count should be > (0L)
  }
  it should "create new tag" in { implicit session =>
    val created = Tags
      .create(name = "foo")
      .get

    created.name shouldBe "foo"
  }

  it should "save a record, updating the modified fields" in { implicit session =>
    val entity = Tags.findAll().head
    val modified = entity.copy(name = "bar")
    val updated = Tags.save(modified).get
    updated.name should equal("bar")
  }

  it should "destroy a record" in { implicit session =>
    val created = Tags
      .create(name = "foo")
      .get
    val deleted = Tags.destroy(created)
    deleted should be(1)
    val shouldBeNone = Tags.find(created.id.get)
    shouldBeNone.isDefined should be(false)
  }

  it should "perform batch insert" in { implicit session =>
    val entities = Tags.findAll()
    entities.foreach(e => Tags.destroy(e))
    val batchInserted = Tags.batchInsert(entities)
    batchInserted.size should be > (0)
  }
}
