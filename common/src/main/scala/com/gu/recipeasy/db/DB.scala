package com.gu.recipeasy.db

import java.sql.Types
import java.time.{ OffsetDateTime, ZoneOffset }
import java.util.Date

import com.gu.recipeas.db.ContextWrapper
import com.gu.recipeasy.models._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._
import org.postgresql.util.PGobject

import scala.reflect.ClassTag

class DB(contextWrapper: ContextWrapper) {
  import contextWrapper.dbContext._

  private implicit val encodePublicationDate = mappedEncoding[OffsetDateTime, Date](d => Date.from(d.toInstant))
  private implicit val decodePublicationDate = mappedEncoding[Date, OffsetDateTime](d => OffsetDateTime.ofInstant(d.toInstant, ZoneOffset.UTC))
  private implicit val encodeStatus = mappedEncoding[RecipeStatus, String](_.toString())
  private implicit val decodeStatus = mappedEncoding[String, RecipeStatus](d => d match {
    case "New" => New
    case "Ready" => Ready
    case "Pending" => Pending
    case "Curated" => Curated
    case "Verified" => Verified
    case "Finalised" => Finalised
    case "Impossible" => Impossible
    case _ => Impossible
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

  private implicit val servesEncoder: Encoder[Serves] = jsonbEncoder[Serves]
  private implicit val detailedServesEncoder: Encoder[DetailedServes] = jsonbEncoder[DetailedServes]
  private implicit val ingredientsListsEncoder: Encoder[IngredientsLists] = jsonbEncoder[IngredientsLists]
  private implicit val detailedIngredientsListsEncoder: Encoder[DetailedIngredientsLists] = jsonbEncoder[DetailedIngredientsLists]
  private implicit val timesEncoder: Encoder[TimesInMins] = jsonbEncoder[TimesInMins]
  private implicit val stepsEncoder: Encoder[Steps] = jsonbEncoder[Steps]
  private implicit val tagsEncoder: Encoder[TagNames] = jsonbEncoder[TagNames]
  private implicit val imagesEncoder: Encoder[Images] = jsonbEncoder[Images]

  private implicit val servesDecoder: Decoder[Serves] = jsonbDecoder[Serves]
  private implicit val detailedServesDecoder: Decoder[DetailedServes] = jsonbDecoder[DetailedServes]
  private implicit val ingredientsListsDecoder: Decoder[IngredientsLists] = jsonbDecoder[IngredientsLists]
  private implicit val detailedIngredientsListsDecoder: Decoder[DetailedIngredientsLists] = jsonbDecoder[DetailedIngredientsLists]
  private implicit val timesDecoder: Decoder[TimesInMins] = jsonbDecoder[TimesInMins]
  private implicit val stepsDecoder: Decoder[Steps] = jsonbDecoder[Steps]
  private implicit val tagsDecoder: Decoder[TagNames] = jsonbDecoder[TagNames]
  private implicit val imagesDecoder: Decoder[Images] = jsonbDecoder[Images]

  //this returns a list of articles with associated recipes
  //NB articles are NOT stored in a DB table but recipes stored have field referencing parent article
  def existingArticlesIds(): List[String] = {
    val action = quote {
      query[Recipe].map(r => r.articleId).distinct
    }
    contextWrapper.dbContext.run(action)
  }

  //an article can have multiple recipes associated with it
  //this collects a list of all articles with at least one recipe which has been edited
  def editedArticlesIds(): List[String] = {
    val action = quote {
      query[Recipe].filter(_.status != "New").map(r => r.articleId).distinct
    }
    contextWrapper.dbContext.run(action)
  }

  def insertAll(recipes: List[Recipe]): Unit = {
    try {
      val action = quote {
        liftQuery(recipes).foreach(r => query[Recipe].insert(r))
      }
      contextWrapper.dbContext.run(action)
    } catch {
      case e: java.sql.BatchUpdateException => throw e.getNextException
    }
  }

  def updateAll(recipes: List[Recipe]): Unit = {
    try {
      val action = quote {
        liftQuery(recipes).foreach(r =>
          query[Recipe].filter(_.id == r.id).update(r))
      }
      contextWrapper.dbContext.run(action)
    } catch {
      case e: java.sql.BatchUpdateException => throw e.getNextException
    }
  }

  def insertImages(images: List[ImageDB]): Unit = {
    val table = quote(query[ImageDB].schema(_.entity("image")))
    try {
      val action = quote {
        liftQuery(images).foreach(i => table.insert(i))
      }
      contextWrapper.dbContext.run(action)
    } catch {
      case e: java.sql.BatchUpdateException => throw e.getNextException
    }
  }

  def getImages(articleId: String): List[ImageDB] = {
    val table = quote(query[ImageDB].schema(_.entity("image")))
    val a = quote {
      table.filter(i => i.articleId == lift(articleId))
    }
    contextWrapper.dbContext.run(a)
  }

  // ---------------------------------------------
  // Original Recipes

  def getRecipes(): List[Recipe] = {
    contextWrapper.dbContext.run(quote(query[Recipe]))
  }

  def getOriginalRecipe(recipeId: String): Option[Recipe] = {
    contextWrapper.dbContext.run(quote(query[Recipe]).filter(r => r.id == lift(recipeId))).headOption
  }

  def getOriginalRecipeInNewStatus(): Option[Recipe] = {
    contextWrapper.dbContext.run(quote(query[Recipe]).filter(r => r.status == "New").sortBy(r => r.publicationDate)(Ord.desc).take(1)).headOption
  }

  def resetOriginalRecipesStatus(): Unit = {
    val a = quote(query[Recipe].filter(_.status == "Pending").update(_.status -> "New"))
    contextWrapper.dbContext.run(a)
  }

  def getOriginalRecipeStatus(recipeId: String): Option[RecipeStatus] = {
    contextWrapper.dbContext.run(quote(query[Recipe]).filter(r => r.id == lift(recipeId))).map(r => r.status).headOption
  }

  def setOriginalRecipeStatus(recipeId: String, s: RecipeStatus): Unit = {
    val a = quote(query[Recipe].filter(r => r.id == lift(recipeId)).update(_.status -> lift(s.toString)))
    contextWrapper.dbContext.run(a)
  }

  def moveStatusForward(recipeId: String): Unit = {
    getOriginalRecipeStatus(recipeId) match {
      case Some(Pending) => setOriginalRecipeStatus(recipeId, Curated)
      case Some(Curated) => setOriginalRecipeStatus(recipeId, Verified)
      case Some(Verified) => setOriginalRecipeStatus(recipeId, Finalised)
      case _ => None
    }
  }

  def countRecipes(): Long = {
    contextWrapper.dbContext.run(quote(query[Recipe])).size
  }

  def countRecipesInGivenStatus(status: RecipeStatus): Long = {
    contextWrapper.dbContext.run(quote(query[Recipe]).filter(r => r.status == lift(status.toString))).size
  }

  def curationCompletionRatio(): Double = {
    (countRecipesInGivenStatus(Pending) + countRecipesInGivenStatus(Curated)) / countRecipes()
  }

  def verificationCompletionRatio(): Double = {
    (countRecipesInGivenStatus(Pending) + countRecipesInGivenStatus(Curated) + countRecipesInGivenStatus(Verified) + countRecipesInGivenStatus(Finalised)) / countRecipes()
  }

  // ---------------------------------------------
  // Curated recipes

  def getCuratedRecipeByRecipeId(recipeId: String): Option[CuratedRecipeDB] = {
    val table = quote(query[CuratedRecipeDB].schema(_.entity("curatedRecipe")))
    val a = quote {
      table.filter(r => r.recipeId == lift(recipeId))
    }
    contextWrapper.dbContext.run(a).headOption
  }

  def getCuratedRecipe(): Option[CuratedRecipeDB] = {
    val table = quote(query[CuratedRecipeDB].schema(_.entity("curatedRecipe")))
    val a = quote {
      (table.sortBy(r => r.id)(Ord.desc).take(1))
    }
    contextWrapper.dbContext.run(a).headOption
  }

  def insertCuratedRecipe(cr: CuratedRecipe): Unit = {
    val table = quote(query[CuratedRecipeDB].schema(_.entity("curated_recipe")))
    val crDB: CuratedRecipeDB = CuratedRecipe.toDBModel(cr)
    try {
      val action = quote {
        table.insert(lift(crDB)).returning(_.id)
      }
      contextWrapper.dbContext.run(action)
    } catch {
      case e: java.sql.BatchUpdateException => throw e.getNextException
    }
  }

  def deleteCuratedRecipeByRecipeId(recipeId: String) = {
    val table = quote(query[CuratedRecipeDB].schema(_.entity("curatedRecipe")))
    val a = quote {
      table.filter(r => r.recipeId == lift(recipeId)).delete
    }
    contextWrapper.dbContext.run(a)
  }

  // ---------------------------------------------
  // User Events

  def countUserEvents(): Long = {
    contextWrapper.dbContext.run(quote(query[UserEvent].schema(_.entity("user_events")))).size
  }

  def insertUserEvent(event: UserEvent): Unit = {
    val table = quote(query[UserEventDB].schema(_.entity("user_events")))
    try {
      val action = quote {
        table.insert(lift(UserEvent.toUserEvents(event)))
      }
      contextWrapper.dbContext.run(action)
    } catch {
      case e: java.sql.BatchUpdateException => throw e.getNextException
    }
  }

  def userEvents(): List[UserEventDB] = {
    contextWrapper.dbContext.run(
      quote(
        query[UserEventDB].schema(_.entity("user_events")).sortBy(event => event.event_datetime)(Ord.desc).take(200)
      )
    )
  }

}
