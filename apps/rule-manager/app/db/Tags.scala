package db
import model.TagForm
import play.api.libs.json.{Format, Json}
import scalikejdbc._
import utils.{DbException, NotFoundException}

import scala.util.Try
import scala.util.{Failure, Success}

case class Tag(
    id: Option[Int],
    name: String
)
object Tags extends SQLSyntaxSupport[Tag] {
  implicit val format: Format[Tag] = Json.format[Tag]

  override val tableName = "tags"

  override val columns = Seq("id", "name")

  val t = Tags.syntax("t")

  def fromResultName(r: ResultName[Tag])(rs: WrappedResultSet): Tag =
    autoConstruct(rs, r)

  def find(id: Int)(implicit session: DBSession = autoSession): Option[Tag] = {
    withSQL {
      select.from(Tags as t).where.eq(t.id, id)
    }.map(Tags.fromResultName(t.resultName)).single().apply()
  }

  def findAll()(implicit session: DBSession = autoSession): List[Tag] = {
    withSQL(select.from(Tags as t).orderBy(t.id))
      .map(Tags.fromResultName(t.resultName))
      .list()
      .apply()
  }

  def countAll()(implicit session: DBSession = autoSession): Long = {
    withSQL(select(sqls.count).from(Tags as t)).map(rs => rs.long(1)).single().apply().get
  }

  def findBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Option[Tag] = {
    withSQL {
      select.from(Tags as t).where.append(where)
    }.map(Tags.fromResultName(t.resultName)).single().apply()
  }

  def findAllBy(where: SQLSyntax)(implicit session: DBSession = autoSession): List[Tag] = {
    withSQL {
      select.from(Tags as t).where.append(where)
    }.map(Tags.fromResultName(t.resultName)).list().apply()
  }

  def countBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Long = {
    withSQL {
      select(sqls.count).from(Tags as t).where.append(where)
    }.map(_.long(1)).single().apply().get
  }

  def create(
      name: String
  )(implicit session: DBSession = autoSession): Try[Tag] = {
    val generatedKey = withSQL {
      insert
        .into(Tags)
        .namedValues(
          column.name -> name
        )
    }.updateAndReturnGeneratedKey().apply()
    find(generatedKey.toInt) match {
      case Some(rule) => Success(rule)
      case None =>
        Failure(
          new Exception(
            s"Attempted to create a tag with id $generatedKey, but no result found attempting to read it back"
          )
        )
    }
  }

  def createFromTagForm(tagForm: TagForm)(implicit
      session: DBSession = autoSession
  ) = {
    Tags.create(
      name = tagForm.name
    )
  }

  def updateFromTagForm(id: Int, tagForm: TagForm)(implicit
      session: DBSession = autoSession
  ) = {
    val tag = Tags
      .find(id)
      .toRight(NotFoundException("Rule not found matching ID"))
      .map(existingRule =>
        existingRule.copy(
          name = tagForm.name
        )
      )
    tag match {
      case Right(tag) => {
        Tags.save(tag) match {
          case Left(e: Exception) => Left(e: Exception)
          case Right(dbRule)      => Right(dbRule)
        }
      }
      case Left(result) => Left(result)
    }
  }

  def batchInsert(
      entities: collection.Seq[Tag]
  )(implicit session: DBSession = autoSession): List[Int] = {
    val params: collection.Seq[Seq[(Symbol, Any)]] = entities.map(entity =>
      Seq(
        Symbol("name") -> entity.name
      )
    )
    SQL(s"""insert into $tableName(
      name
    ) values (
      {name}
    )""").batchByName(params.toSeq: _*).apply[List]()
  }

  def save(entity: Tag)(implicit session: DBSession = autoSession): Either[DbException, Tag] = {
    withSQL {
      update(Tags)
        .set(
          column.id -> entity.id,
          column.name -> entity.name
        )
        .where
        .eq(column.id, entity.id)
    }.update().apply()

    find(entity.id.get)
      .toRight(
        DbException(s"Error updating rule with id ${entity.id}: could not read updated rule")
      )
  }

  def destroy(entity: Tag)(implicit session: DBSession = autoSession): Int = {
    withSQL {
      delete.from(Tags).where.eq(column.id, entity.id)
    }.update().apply()
  }

  def destroyAll()(implicit session: DBSession = autoSession): Int = {
    withSQL {
      delete.from(Tags)
    }.update().apply()
  }
}
