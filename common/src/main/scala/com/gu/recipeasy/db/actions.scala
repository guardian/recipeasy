package com.gu.recipeasy.db

import java.sql.Types
import java.time.OffsetDateTime
import java.util.Date

import com.gu.recipeasy.models._
import io.circe.generic.auto._
import io.circe.syntax._
import io.getquill._
import org.postgresql.util.PGobject

import scala.reflect.ClassTag

object db {

  lazy val ctx = new JdbcContext[PostgresDialect, SnakeCase]("db.ctx")
  import ctx._

  implicit val encodePublicationDate = mappedEncoding[OffsetDateTime, Date](d => Date.from(d.toInstant))
  implicit val encodeStatus = mappedEncoding[Status, String](_.toString())

  private def jsonbEncoder[T: io.circe.Encoder: ClassTag]: Encoder[T] = {
    encoder[T]({ row => (idx, value) =>
      val pgObj = new PGobject()
      pgObj.setType("jsonb")
      pgObj.setValue(value.asJson.noSpaces)
      row.setObject(idx, pgObj, Types.OTHER)
    }, Types.OTHER)
  }

  implicit val servesEncoder: Encoder[Serves] = jsonbEncoder[Serves]
  implicit val stepsEncoder: Encoder[Steps] = jsonbEncoder[Steps]
  implicit val ingredientsListsEncoder: Encoder[IngredientsLists] = jsonbEncoder[IngredientsLists]

  def insertAll(recipes: List[Recipe]): Unit = {
    try {
      val action = quote {
        liftQuery(recipes).foreach(r => query[Recipe].insert(r))
      }
      ctx.run(action)
    } catch {
      case e: java.sql.BatchUpdateException => throw e.getNextException
    }
  }

}
