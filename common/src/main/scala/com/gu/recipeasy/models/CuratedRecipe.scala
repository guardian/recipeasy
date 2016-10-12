package com.gu.recipeasy.models

import automagic._
import io.circe._
import cats.data.Xor
import CuratedRecipeDB._
import ImageDB._

case class CuratedRecipe(
  id: Long,
  recipeId: String,
  title: String,
  serves: Option[DetailedServes],
  ingredientsLists: DetailedIngredientsLists,
  credit: Option[String],
  times: TimesInMins,
  steps: Steps,
  tags: Tags,
  images: Images

)

case class DetailedServes(
  portion: PortionType,
  quantity: Serves
)

object DetailedServes {
  def fromServes(serves: Option[Serves]): Option[DetailedServes] = {
    serves.map(s => DetailedServes(ServesType, s))
  }
}

sealed trait PortionType

case object MakesType extends PortionType
case object ServesType extends PortionType

object PortionType {
  def fromString(s: String): PortionType = {
    s match {
      case "ServesType" => ServesType
      case "MakesType" => MakesType
    }
  }

  implicit val circeEncoder: Encoder[PortionType] = Encoder.encodeString.contramap[PortionType](_.toString)
  implicit val circeDecoder: Decoder[PortionType] = Decoder.decodeString.emap { str =>
    str match {
      case "ServesType" => Xor.right(ServesType)
      case "MakesType" => Xor.right(MakesType)
    }
  }
}

case class DetailedIngredientsLists(lists: Seq[DetailedIngredientsList])

object DetailedIngredientsLists {
  def fromIngredientsLists(ingredients: IngredientsLists): DetailedIngredientsLists = {
    new DetailedIngredientsLists(lists =
      ingredients.lists.map(r => new DetailedIngredientsList(r.title, rawToDetailedIngredients(r.ingredients))))
  }
  private def rawToDetailedIngredients(ingredients: Seq[String]): Seq[DetailedIngredient] = {
    ingredients.map(i => new DetailedIngredient(None, None, "", None, i))
  }
}

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
  val category = Seq("Baking", "Barbecue", "Breakfast", "Budget", "Canapes", "Dessert", "Dinner party", "Drinks and cockails", "Healthy eating", "Lunch", "Main course", "Picnic", "Sides", "Snacks", "Starters")
  val holidays = Seq("Baisakhi", "Christmas", "Diwali", "Easter", "Eid", "Halloween", "Hanukkah", "Passover", "Thanksgiving")
  val dietary = Seq("Low sugar", "Low fat", "High fibre", "Nut free", "Gluten free", "Dairy free", "Egg free", "Vegetarian", "Vegan")
}

case class Images(images: Seq[Image])

case class Image(
  mediaId: String,
  assetUrl: String,
  altText: String
)

object Image {
  def fromImageDB(i: ImageDB): Image = {
    transform[ImageDB, Image](i)
  }
}

object CuratedRecipe {

  def fromRecipe(r: Recipe): CuratedRecipe = {
    transform[Recipe, CuratedRecipe](
      r,
      "id" -> 0L,
      "recipeId" -> r.id,
      "serves" -> DetailedServes.fromServes(r.serves),
      "times" -> TimesInMins(None, None),
      "tags" -> Tags(List.empty),
      "ingredientsLists" -> DetailedIngredientsLists.fromIngredientsLists(r.ingredientsLists),
      "images" -> Images(List.empty)
    )
  }

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

