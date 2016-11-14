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
  def displayName: String
}

object CookingUnit {
  val unitMap = Map(
    Cup.abbreviation -> Cup,
    Gram.abbreviation -> Gram,
    Kilogram.abbreviation -> Kilogram,
    Ounce.abbreviation -> Ounce,
    Pound.abbreviation -> Pound,
    Bottle.abbreviation -> Bottle,
    FluidOunce.abbreviation -> FluidOunce,
    Litre.abbreviation -> Litre,
    Millilitre.abbreviation -> Millilitre,
    Tablespoon.abbreviation -> Tablespoon,
    Teaspoon.abbreviation -> Teaspoon,
    Bunch.abbreviation -> Bunch,
    Centimetre.abbreviation -> Centimetre,
    Can.abbreviation -> Can,
    Clove.abbreviation -> Clove,
    Dash.abbreviation -> Dash,
    Grating.abbreviation -> Grating,
    Handful.abbreviation -> Handful,
    Packet.abbreviation -> Packet,
    Piece.abbreviation -> Piece,
    Pinch.abbreviation -> Pinch,
    Sheet.abbreviation -> Sheet,
    Sprig.abbreviation -> Sprig,
    Stick.abbreviation -> Stick
  )

  def fromString(s: String): Option[CookingUnit] = {
    unitMap.get(s)
  }

  //UI select option lists
  def Weights: List[CookingUnit] = List(Cup, Gram, Kilogram, Ounce, Pound)
  def Liquids: List[CookingUnit] = List(Bottle, FluidOunce, Litre, Millilitre)
  def Spoons: List[CookingUnit] = List(Dessert, Tablespoon, Teaspoon)
  def Other: List[CookingUnit] = List(Bunch, Centimetre, Can, Clove, Dash, Grating, Handful, Packet, Piece, Pinch, Sheet, Sprig, Stick)

  implicit val circeEncoder: Encoder[CookingUnit] = Encoder.encodeString.contramap[CookingUnit](_.abbreviation)
  implicit val circeDecoder: Decoder[CookingUnit] = Decoder.decodeString.emap { str =>
    fromString(str) match {
      case Some(x) => Xor.right(x)
      case None => Xor.left("Cannot decode unrecognised Cooking Unit")
    }
  }

}

case object Cup extends CookingUnit { val abbreviation = "cup"; val displayName = "Cup" }
case object Gram extends CookingUnit { val abbreviation = "g"; val displayName = "Gram (g)" }
case object Kilogram extends CookingUnit { val abbreviation = "kg"; val displayName = "Kilogram (kg)" }
case object Ounce extends CookingUnit { val abbreviation = "oz"; val displayName = "Ounce (oz)" }
case object Pound extends CookingUnit { val abbreviation = "lb"; val displayName = "Pound (lb)" }

case object Bottle extends CookingUnit { val abbreviation = "bottle"; val displayName = "Bottle" }
case object FluidOunce extends CookingUnit { val abbreviation = "floz"; val displayName = "Fluid Ounce (fl oz)" }
case object Litre extends CookingUnit { val abbreviation = "l"; val displayName = "Litre (l)" }
case object Millilitre extends CookingUnit { val abbreviation = "ml"; val displayName = "Millilitre (ml)" }

case object Dessert extends CookingUnit { val abbreviation = "dsp"; val displayName = "Dessert spoon (dsp)" }
case object Teaspoon extends CookingUnit { val abbreviation = "tsp"; val displayName = "Teaspoon (tsp)" }
case object Tablespoon extends CookingUnit { val abbreviation = "tbsp"; val displayName = "Tablespoon (tbsp)" }

case object Bunch extends CookingUnit { val abbreviation = "bunch"; val displayName = "Bunch" }
case object Centimetre extends CookingUnit { val abbreviation = "cm"; val displayName = "Centimetre (cm)" }
case object Can extends CookingUnit { val abbreviation = "can"; val displayName = "Can" }
case object Clove extends CookingUnit { val abbreviation = "clove"; val displayName = "Clove" }
case object Dash extends CookingUnit { val abbreviation = "dash"; val displayName = "Dash" }
case object Grating extends CookingUnit { val abbreviation = "grating"; val displayName = "Grating" }
case object Handful extends CookingUnit { val abbreviation = "handful"; val displayName = "Handful" }
case object Packet extends CookingUnit { val abbreviation = "Packet"; val displayName = "Packet" }
case object Piece extends CookingUnit { val abbreviation = "piece"; val displayName = "Piece" }
case object Pinch extends CookingUnit { val abbreviation = "pinch"; val displayName = "Pinch" }
case object Sheet extends CookingUnit { val abbreviation = "sheet"; val displayName = "Sheet" }
case object Sprig extends CookingUnit { val abbreviation = "sprig"; val displayName = "Sprig" }
case object Stick extends CookingUnit { val abbreviation = "stick"; val displayName = "Stick" }
//if a new case object is added it must be added to UI select option list and unitMap above

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
    transform[CuratedRecipeDB, CuratedRecipe](
      r,
      "tags" -> getFullTags(r.tags)
    )
  }

  def getTagNames(tags: Tags): TagNames = {
    TagNames(tags.list.map(t => t.name))
  }

  def getFullTags(tags: TagNames): Tags = {
    Tags(tags.list.collect {
      case t if Tag.cuisines.contains(t) => Tag(t, "cuisines")
      case t if Tag.category.contains(t) => Tag(t, "category")
      case t if Tag.holidays.contains(t) => Tag(t, "holidays")
      case t if Tag.dietary.contains(t) => Tag(t, "dietary")
    })
  }

}

