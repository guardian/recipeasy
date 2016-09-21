package com.gu.recipeasy.models

import java.time.OffsetDateTime
import automagic._
import io.circe._
import cats.data.Xor
import CuratedRecipeDB._

case class CuratedRecipe(
  id: String,
  title: String,
  body: String,
  serves: Option[Serves],
  ingredientsLists: DetailedIngredientsLists,
  articleId: String,
  credit: Option[String],
  publicationDate: OffsetDateTime,
  status: Status,
  times: TimesInMins,
  steps: Steps,
  tags: Tags
)

case class DetailedIngredientsLists(lists: Seq[DetailedIngredientsList])

case class DetailedIngredientsList(
  title: Option[String],
  ingredients: Seq[DetailedIngredient]
)

case class DetailedIngredient(
  quantity: Option[Double],
  unit: Option[CookingUnit],
  item: String,
  comment: Option[String],
  raw: String
)

case class TimesInMins(
  preparation: Option[Double],
  cooking: Option[Double]
)

sealed trait CookingUnit {
  def abbreviation: String
}

object CookingUnit {
  val unitMap = Map(
    "g" -> Gram,
    "ml" -> Milliltre,
    "l" -> Litre,
    "oz" -> Ounces,
    "floz" -> FluidOunces,
    "cup" -> Cup,
    "tsp" -> Teaspoon,
    "tbsp" -> Tablespoon,
    "pinch" -> Pinch,
    "handful" -> Handful,
    "grating" -> Grating
  )

  def fromString(s: String): Option[CookingUnit] = {
    unitMap.get(s)
  }

  implicit val circeEncoder: Encoder[CookingUnit] = Encoder.encodeString.contramap[CookingUnit](_.abbreviation)
  implicit val circeDecoder: Decoder[CookingUnit] = Decoder.decodeString.emap { str =>
    fromString(str) match {
      case Some(x) => Xor.right(x)
      case None => Xor.left("Cannot decode unrecognised Cooking Unit")
    }
  }

}

case object Gram extends CookingUnit { def abbreviation = "g" }
case object Milliltre extends CookingUnit { def abbreviation = "ml" }
case object Litre extends CookingUnit { def abbreviation = "l" }
case object Ounces extends CookingUnit { def abbreviation = "oz" }
case object FluidOunces extends CookingUnit { def abbreviation = "floz" }
case object Cup extends CookingUnit { def abbreviation = "cup" }
case object Teaspoon extends CookingUnit { def abbreviation = "tsp" }
case object Tablespoon extends CookingUnit { def abbreviation = "tbsp" }
case object Pinch extends CookingUnit { def abbreviation = "pinch" }
case object Handful extends CookingUnit { def abbreviation = "handful" }
case object Grating extends CookingUnit { def abbreviation = "grating" }

case class Tags(list: Seq[Tag])

case class Tag(
  name: String,
  category: String
)

object Tag {
  val cuisines = Seq("African", "British", "Caribbean", "French", "Greek", "Indian", "Irish", "Italian", "Japanese", "Mexican", "Nordic", "North African", "Portuguese", "South American", "Spanish", "Thai and South East Asian")
  val mealTypes = Seq("Barbecue", "Breakfast", "Budget", "Canapes", "Dessert", "Dinner party", "Drinks and cockails", "Healthy eating", "Lunch", "Main course", "Picnic", "Sides", "Snacks", "Starters")
  val holidays = Seq("Baisakhi", "Christmas", "Diwali", "Easter", "Eid", "Halloween", "Hanukkah", "Passover", "Thanksgiving")
  val dietary = Seq("Low sugar", "Low fat", "High fibre", "Nut free", "Gluten free", "Dairy free", "Egg free", "Vegetarian", "Vegan")
}

object CuratedRecipe {
  import CuratedRecipeDB._

  def toDBModel(cr: CuratedRecipe): CuratedRecipeDB = {
    transform[CuratedRecipe, CuratedRecipeDB](
      cr,
      "tags" -> getTagNames(cr.tags)
    )
  }

  def fromCuratedRecipeDB(r: CuratedRecipeDB): CuratedRecipe = {
    ???
  }

  def getTagNames(tags: Tags): TagNames = {
    TagNames(tags.list.map(t => t.name))
  }

}

