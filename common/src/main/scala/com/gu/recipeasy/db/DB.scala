package com.gu.recipeasy.db

import java.sql.Types
import java.time.{ OffsetDateTime, ZoneOffset }
import java.util.Date

import com.gu.recipeasy.models._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._
import io.getquill._
import org.postgresql.util.PGobject

import scala.reflect.ClassTag

object DB {

  lazy val ctx = new JdbcContext[PostgresDialect, SnakeCase]("db.ctx")
  import ctx._

  implicit val encodePublicationDate = mappedEncoding[OffsetDateTime, Date](d => Date.from(d.toInstant))
  implicit val encodeStatus = mappedEncoding[Status, String](_.toString())
  implicit val decodePublicationDate = mappedEncoding[Date, OffsetDateTime](d => OffsetDateTime.ofInstant(d.toInstant, ZoneOffset.UTC))
  implicit val decodeStatus = mappedEncoding[String, Status](d => d match {
    case "New" => New
    case "Curated" => Curated
    case "Impossible" => Impossible
  })

  private def jsonbEncoder[T: io.circe.Encoder: ClassTag]: Encoder[T] = {
    encoder[T]({ row => (idx, value) =>
      val pgObj = new PGobject()
      pgObj.setType("jsonb")
      pgObj.setValue(value.asJson.noSpaces)
      row.setObject(idx, pgObj, Types.OTHER)
    }, Types.OTHER)
  }

  private def jsonbDecoder[T <: AnyRef: io.circe.Decoder: ClassTag]: Decoder[T] = {
    decoder[T]({ row => idx =>
      val pgObj = new PGobject()
      pgObj.setType("jsonb")
      val jsonString = row.getObject(idx).asInstanceOf[PGobject]
      if (jsonString == null) jsonString.asInstanceOf[T]
      else {
        val json = parse(jsonString.getValue).getOrElse(Json.Null)
        json.as[T].getOrElse(throw new RuntimeException)
      }
    })
  }

  implicit val servesEncoder: Encoder[Serves] = jsonbEncoder[Serves]
  implicit val stepsEncoder: Encoder[Steps] = jsonbEncoder[Steps]
  implicit val ingredientsListsEncoder: Encoder[IngredientsLists] = jsonbEncoder[IngredientsLists]
  implicit val servesDecoder: Decoder[Serves] = jsonbDecoder[Serves]
  implicit val stepsDecoder: Decoder[Steps] = jsonbDecoder[Steps]
  implicit val ingredientsListsDecoder: Decoder[IngredientsLists] = jsonbDecoder[IngredientsLists]

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

  def getNewRecipe: Option[Recipe] = {
    ctx.run(quote(query[Recipe]).filter(r => r.status == "New").sortBy(r => r.publicationDate).take(1)).headOption
  }

}
