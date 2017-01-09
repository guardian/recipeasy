package com.gu.recipeasy.db

import java.sql.Types
import java.time.{ OffsetDateTime, ZoneOffset }
import java.util.Date

import com.gu.recipeasy.ProgressCache
import com.gu.recipeasy.models._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._
import org.postgresql.util.PGobject
import scala.collection.JavaConverters._

import scala.util.Random

import scala.reflect.ClassTag

class DB(contextWrapper: ContextWrapper) {
  import contextWrapper.dbContext._

  private implicit val encodePublicationDate = mappedEncoding[OffsetDateTime, Date](d => Date.from(d.toInstant))
  private implicit val decodePublicationDate = mappedEncoding[Date, OffsetDateTime](d => OffsetDateTime.ofInstant(d.toInstant, ZoneOffset.UTC))
  private implicit val encodeStatus = mappedEncoding[RecipeStatus, String](_.toString())
  private implicit val decodeStatus = mappedEncoding[String, RecipeStatus](d => d match {
    case "New" => RecipeStatusNew
    case "Ready" => RecipeStatusReady
    case "Pending" => RecipeStatusPendingCuration
    case "Curated" => RecipeStatusCurated
    case "Verified" => RecipeStatusVerified
    case "Finalised" => RecipeStatusFinalised
    case "Impossible" => RecipeStatusImpossible
    case _ => RecipeStatusImpossible
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
      query[Recipe].filter(_.status != lift(RecipeStatusNew.name)).map(r => r.articleId).distinct
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

  def getOriginalRecipes(): List[Recipe] = {
    contextWrapper.dbContext.run(quote(query[Recipe]))
  }

  def getOriginalRecipe(recipeId: String): Option[Recipe] = {
    contextWrapper.dbContext.run(quote(query[Recipe]).filter(r => r.id == lift(recipeId))).headOption
  }

  def getOriginalRecipeInNewStatus(): Option[Recipe] = {
    contextWrapper.dbContext.run(quote(query[Recipe]).filter(r => r.status == lift(RecipeStatusNew.name)).sortBy(r => r.publicationDate)(Ord.desc).take(1)).headOption
  }

  def getOriginalRecipeInReadyStatus(): Option[Recipe] = {
    contextWrapper.dbContext.run(quote(query[Recipe]).filter(r => r.status == lift(RecipeStatusReady.name)).sortBy(r => r.publicationDate)(Ord.desc).take(1)).headOption
  }

  def getOriginalRecipeInVerifiableStatus(): Option[Recipe] = {
    contextWrapper.dbContext.run(quote(query[Recipe]).filter(r => (r.status == lift(RecipeStatusCurated.name) || r.status == lift(RecipeStatusVerified.name))).sortBy(r => r.publicationDate)(Ord.desc).take(1)).headOption
  }

  def resetOriginalRecipesInPendingStatuses(): Unit = {
    val a = quote(query[Recipe].filter(_.status == lift(RecipeStatusPendingCuration.name)).update(_.status -> lift(RecipeStatusReady.name)))
    contextWrapper.dbContext.run(a)
    val b = quote(query[Recipe].filter(_.status == lift(RecipeStatusPendingVerification.name)).update(_.status -> lift(RecipeStatusCurated.name)))
    contextWrapper.dbContext.run(b)
    val c = quote(query[Recipe].filter(_.status == lift(RecipeStatusPendingFinalisation.name)).update(_.status -> lift(RecipeStatusVerified.name)))
    contextWrapper.dbContext.run(c)
  }

  def getOriginalRecipeStatus(recipeId: String): Option[RecipeStatus] = {
    contextWrapper.dbContext.run(quote(query[Recipe]).filter(r => r.id == lift(recipeId))).map(r => r.status).headOption
  }

  def setOriginalRecipeStatus(recipeId: String, s: RecipeStatus): Unit = {
    val a = quote(query[Recipe].filter(r => r.id == lift(recipeId)).update(_.status -> lift(s.name)))
    contextWrapper.dbContext.run(a)
  }

  def moveStatusForward(recipeId: String): Unit = {
    getOriginalRecipeStatus(recipeId) match {
      case Some(RecipeStatusPendingCuration) => setOriginalRecipeStatus(recipeId, RecipeStatusCurated)
      case Some(RecipeStatusCurated) => setOriginalRecipeStatus(recipeId, RecipeStatusVerified)
      case Some(RecipeStatusVerified) => setOriginalRecipeStatus(recipeId, RecipeStatusFinalised)
      case _ => None
    }
  }

  def countRecipes(): Long = {
    contextWrapper.dbContext.run(quote(query[Recipe])).size
  }

  def countRecipesInGivenStatus(status: RecipeStatus): Long = {
    contextWrapper.dbContext.run(quote(query[Recipe]).filter(r => r.status == lift(status.name))).size
  }

  def progressBarRatio(): Double = {

    // progressBarRatio ratio is a number between 0 and 1

    def pbr(): Double = {

      // We the advanced index counts steps defined as status updates
      //      New -> Ready -> Pending -> Curated -> Verified -> Finalised
      //      Five migrations

      // We apply this to the entire database (including the New) elements, minus the Impossible ones; what we refer to as "alive" recipes below

      val newCount = countRecipesInGivenStatus(RecipeStatusNew)
      val readyCount = countRecipesInGivenStatus(RecipeStatusReady)
      val pendingCount = countRecipesInGivenStatus(RecipeStatusPendingCuration)
      val curatedCount = countRecipesInGivenStatus(RecipeStatusCurated)
      val verifiedCount = countRecipesInGivenStatus(RecipeStatusVerified)
      val finalisedCount = countRecipesInGivenStatus(RecipeStatusFinalised)

      val aliveRecipesCount: Long = newCount + readyCount + pendingCount + curatedCount + verifiedCount + finalisedCount
      val possibleMigrationsCount: Long = aliveRecipesCount * 5
      val remainingMigrationCount: Long = newCount * 5 + readyCount * 4 + pendingCount * 3 + curatedCount * 2 + verifiedCount * 1 + finalisedCount * 0

      if (remainingMigrationCount == 0) {
        0.toDouble
      } else {
        (possibleMigrationsCount - remainingMigrationCount).toDouble / possibleMigrationsCount
      }
    }

    val Key = "verificationCompletionRatio"
    val ratio = ProgressCache.get(Key).getOrElse {
      val result: java.lang.Double = pbr()
      ProgressCache.put(Key, result)
      result
    }
    ratio

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

  val isoDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd")

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

  def userEvents(size: Int): List[UserEventDB] = {
    contextWrapper.dbContext.run(
      quote(
        query[UserEventDB].schema(_.entity("user_events")).sortBy(event => event.event_datetime)(Ord.desc).take(lift(size))
      )
    )
  }

  def userEventsAll(): List[UserEventDB] = {
    contextWrapper.dbContext.run(
      quote(
        query[UserEventDB].schema(_.entity("user_events")).sortBy(event => event.event_datetime)(Ord.desc)
      )
    )
  }

  def userEmails(): List[String] = {
    contextWrapper.dbContext.run(
      quote(
        query[UserEventDB].schema(_.entity("user_events")).map(event => event.user_email).distinct
      )
    )
  }

  def usersCount(): Long = {
    contextWrapper.dbContext.run(
      quote(
        query[UserEventDB].schema(_.entity("user_events")).map(event => event.user_email).distinct.size
      )
    )
  }

  def userEventsDates(): List[String] = {
    contextWrapper.dbContext.run(
      quote(
        query[UserEventDB].schema(_.entity("user_events")).map(event => event.event_datetime)
      )
    ).map(datetime => isoDateFormat.format(datetime)).distinct
  }

  def eventsForDateAndOperationType(date: String, opType: UserEventOperationType): List[UserEventDB] = {

    contextWrapper.dbContext.run(
      quote(
        query[UserEventDB].schema(_.entity("user_events"))
      )
    ).filter(event => (isoDateFormat.format(event.event_datetime) == date) && (event.operation_type == opType.name))
  }

  def dailyActivityDistribution(): List[DayActivityDistribution] = {
    userEventsDates().sorted.map(date => DayActivityDistribution(date, eventsForDateAndOperationType(date, UserEventCuration).size, eventsForDateAndOperationType(date, UserEventVerification).size, eventsForDateAndOperationType(date, UserEventConfirmation).size))
  }

  def getFirstCurationEventForRecipe(recipeId: String): Option[UserEventDB] = {
    val q = quote {
      query[UserEventDB].schema(_.entity("user_events"))
        .filter(event => event.recipe_id == lift(recipeId))
        .filter(event => event.operation_type == lift(UserEventCuration.name))
        .sortBy(event => event.event_datetime)(Ord.asc)
    }
    contextWrapper.dbContext.run(q).headOption
  }

  // ---------------------------------------------
  // Stats

  def generalStats(): Map[GeneralStatisticsPoint, Long] = {
    Map(
      GStatsUserParticipationCount -> usersCount(),
      GStatsCuratedRecipesCount -> (countRecipesInGivenStatus(RecipeStatusCurated) + countRecipesInGivenStatus(RecipeStatusVerified) + countRecipesInGivenStatus(RecipeStatusFinalised)),
      GStatsFinalisedRecipesCount -> countRecipesInGivenStatus(RecipeStatusFinalised),
      GStatsTotalActiveRecipesCount -> (countRecipes() - countRecipesInGivenStatus(RecipeStatusImpossible))
    )
  }

  def userStatsCurationCount(userEmailAddress: String): Long = {
    contextWrapper.dbContext.run(
      quote(
        query[UserEventDB].schema(_.entity("user_events")).filter(event => (event.user_email == lift(userEmailAddress)) && (event.operation_type == lift(UserEventCuration.name))).size
      )
    )
  }
  def userStatsVerificationCount(userEmailAddress: String): Long = {
    contextWrapper.dbContext.run(
      quote(
        query[UserEventDB].schema(_.entity("user_events")).filter(event => (event.user_email == lift(userEmailAddress)) && (event.operation_type == lift(UserEventVerification.name))).size
      )
    )
  }
  def userStatsFinalisationCount(userEmailAddress: String): Long = {
    contextWrapper.dbContext.run(
      quote(
        query[UserEventDB].schema(_.entity("user_events")).filter(event => (event.user_email == lift(userEmailAddress)) && (event.operation_type == lift(RecipeStatusFinalised.name))).size
      )
    )
  }
  def userStatsBiggestDayDate(userEmailAddress: String): String = {
    "7th December" // TODO
  }
  def userStatsBiggestDayCount(userEmailAddress: String): Long = {
    12 // TODO
  }
  def userStatsRanking(userEmailAddress: String): String = {
    "Top 20% of contributors!" // TODO
  }

  def userStats(userEmailAddress: String): Map[PersonalStatisticsPoint, String] = {
    Map(
      PStatsCurationCount -> userStatsCurationCount(userEmailAddress).toString,
      PStatsVerificationCount -> userStatsVerificationCount(userEmailAddress).toString,
      PStatsFinalisationCount -> userStatsFinalisationCount(userEmailAddress).toString,
      PStatsBiggestDayDate -> userStatsBiggestDayDate(userEmailAddress),
      PStatsBiggestDayCount -> userStatsBiggestDayCount(userEmailAddress).toString,
      PStatsRanking -> userStatsRanking(userEmailAddress)
    )
  }

  def getUserSpecificRecipeForVerificationStep(userEmail: String): Option[Recipe] = {
    val recipeIdsAlreadyTouchedByThisUser = quote {
      query[UserEventDB].schema(_.entity("user_events"))
        .filter(event => event.user_email == lift(userEmail))
        .filter(event => ((event.operation_type == lift(UserEventCuration.name)) || (event.operation_type == lift(UserEventVerification.name)) || (event.operation_type == lift(UserEventConfirmation.name))))
        .map(event => event.recipe_id)
    }
    val q2 = quote {
      query[Recipe]
        .filter(r => ((r.status == lift(RecipeStatusCurated.name)) || (r.status == lift(RecipeStatusVerified.name))))
        .filter(r => !recipeIdsAlreadyTouchedByThisUser.contains(r.id))
    }
    val recipes: List[Recipe] = contextWrapper.dbContext.run(q2)
    if (recipes.size == 0) {
      None
    } else {
      Some(Random.shuffle(recipes).head)
    }
  }

}
