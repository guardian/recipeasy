package com.gu.recipeasy.models

import java.time.OffsetDateTime
import automagic._

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
  times: Times,
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

case class Times(
  preparation: Option[Time],
  cooking: Option[Time]
)

case class Time(
  quantity: Double,
  unit: String
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
  val lowSugar = Tag("low sugar", "dietary")
  val lowFat = Tag("low fat", "dietary")
  val highFibre = Tag("high fibre", "dietary")
  val nutFree = Tag("nut free", "dietary")
  val glutenFree = Tag("gluten free", "dietary")
  val dairyFree = Tag("dairy free", "dietary")
  val eggFree = Tag("egg free", "dietary")
  val vegetarian = Tag("vegetarian", "dietary")
  val vegan = Tag("vegan", "dietary")

}

object CuratedRecipe {

  case class TagNames(list: Seq[String])
  //The DB model stores Tag names, e.g. "vegan", rather than full Tag objects, e.g. {"name": "vegan", "category": "dietary"}.
  //Otherwise it is the same as the CuratedRecipe case class.
  case class DBModel(
    id: String,
    title: String,
    body: String,
    serves: Option[Serves],
    ingredientsLists: DetailedIngredientsLists,
    articleId: String,
    credit: Option[String],
    publicationDate: OffsetDateTime,
    status: Status,
    times: Times,
    steps: Steps,
    tags: TagNames
  )

  def toDBModel(cr: CuratedRecipe): DBModel = {
    transform[CuratedRecipe, DBModel](
      cr,
      "tags" -> getTagNames(cr.tags)
    )
  }

  def fromDBModel(r: DBModel): CuratedRecipe = {
    ???
  }

  def getTagNames(tags: Tags): TagNames = {
    TagNames(tags.list.map(t => t.name))
  }

}
