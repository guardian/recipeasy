package com.gu.recipeasy.models

import automagic._
import io.circe._
import cats.data.Xor
import CuratedRecipeDB._
import ImageDB._

case class DetailedServes(
  portion: PortionType,
  quantity: Serves,
  unit: Option[String]
)

object DetailedServes {
  def fromServes(serves: Option[Serves]): Option[DetailedServes] = {
    serves.map(s => DetailedServes(ServesType, s, None))
  }
}

sealed trait PortionType

case object MakesType extends PortionType
case object ServesType extends PortionType
case object QuantityType extends PortionType

object PortionType {
  def fromString(s: String): PortionType = {
    s match {
      case "ServesType" => ServesType
      case "MakesType" => MakesType
      case "QuantityType" => QuantityType
    }
  }

  implicit val circeEncoder: Encoder[PortionType] = Encoder.encodeString.contramap[PortionType](_.toString)
  implicit val circeDecoder: Decoder[PortionType] = Decoder.decodeString.emap { str =>
    str match {
      case "ServesType" => Xor.right(ServesType)
      case "MakesType" => Xor.right(MakesType)
      case "QuantityType" => Xor.right(QuantityType)
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

object TimesInMinsAdapted {
  def preparationTimeInMinutes(cr: CuratedRecipe): Int = {
    cr.times.preparationHours.getOrElse(0) * 60 + cr.times.preparationMinutes.getOrElse(0)
  }
  def cookingTimeInMinutes(cr: CuratedRecipe): Int = {
    cr.times.cookingHours.getOrElse(0) * 60 + cr.times.cookingMinutes.getOrElse(0)
  }
  def normalisedTimes(cr: CuratedRecipe): TimesInMinsAdapted = {
    TimesInMinsAdapted(
      Some(preparationTimeInMinutes(cr) / 60),
      Some(preparationTimeInMinutes(cr) % 60),
      Some(cookingTimeInMinutes(cr) / 60),
      Some(cookingTimeInMinutes(cr) % 60)
    )
  }
}

case class TimesInMinsAdapted(
  preparationHours: Option[Int],
  preparationMinutes: Option[Int],
  cookingHours: Option[Int],
  cookingMinutes: Option[Int]
)

sealed trait CookingUnit {
  def abbreviation: String
  def displayName: String
}

object CookingUnit {
  val unitMap = Map(
    Bottle.abbreviation -> Bottle,
    Bunch.abbreviation -> Bunch,
    Can.abbreviation -> Can,
    Centimetre.abbreviation -> Centimetre,
    Clove.abbreviation -> Clove,
    Cup.abbreviation -> Cup,
    Dash.abbreviation -> Dash,
    FluidOunce.abbreviation -> FluidOunce,
    Gram.abbreviation -> Gram,
    Grating.abbreviation -> Grating,
    Handful.abbreviation -> Handful,
    Head.abbreviation -> Head,
    Inch.abbreviation -> Inch,
    Kilogram.abbreviation -> Kilogram,
    Knob.abbreviation -> Knob,
    Litre.abbreviation -> Litre,
    Millilitre.abbreviation -> Millilitre,
    Millimetre.abbreviation -> Millimetre,
    Ounce.abbreviation -> Ounce,
    Packet.abbreviation -> Packet,
    Piece.abbreviation -> Piece,
    Pinch.abbreviation -> Pinch,
    Pound.abbreviation -> Pound,
    Sheet.abbreviation -> Sheet,
    Slice.abbreviation -> Slice,
    Splash.abbreviation -> Splash,
    Sprig.abbreviation -> Sprig,
    Stick.abbreviation -> Stick,
    Tablespoon.abbreviation -> Tablespoon,
    Teaspoon.abbreviation -> Teaspoon
  )

  def fromString(s: String): Option[CookingUnit] = {
    unitMap.get(s)
  }

  //UI select option lists
  def Weights: List[CookingUnit] = List(Cup, Gram, Kilogram, Ounce, Pound)
  def Liquids: List[CookingUnit] = List(Bottle, FluidOunce, Litre, Millilitre)
  def Spoons: List[CookingUnit] = List(Dessert, Tablespoon, Teaspoon)
  def Natural: List[CookingUnit] = List(Bunch, Clove, Head, Knob, Piece, Sprig, Stick)
  def ByHand: List[CookingUnit] = List(Dash, Pinch, Grating, Handful, Slice, Splash)
  def Packaged: List[CookingUnit] = List(Can, Packet, Sheet)
  def Lengths: List[CookingUnit] = List(Centimetre, Inch, Millimetre)

  implicit val circeEncoder: Encoder[CookingUnit] = Encoder.encodeString.contramap[CookingUnit](_.abbreviation)
  implicit val circeDecoder: Decoder[CookingUnit] = Decoder.decodeString.emap { str =>
    fromString(str) match {
      case Some(x) => Xor.right(x)
      case None => Xor.left("Cannot decode unrecognised Cooking Unit")
    }
  }

}

case object Bottle extends CookingUnit { val abbreviation = "bottle"; val displayName = "Bottle" }
case object Bunch extends CookingUnit { val abbreviation = "bunch"; val displayName = "Bunch" }
case object Can extends CookingUnit { val abbreviation = "can"; val displayName = "Can" }
case object Centimetre extends CookingUnit { val abbreviation = "cm"; val displayName = "Centimetre (cm)" }
case object Clove extends CookingUnit { val abbreviation = "clove"; val displayName = "Clove" }
case object Cup extends CookingUnit { val abbreviation = "cup"; val displayName = "Cup" }
case object Dash extends CookingUnit { val abbreviation = "dash"; val displayName = "Dash" }
case object Dessert extends CookingUnit { val abbreviation = "dsp"; val displayName = "Dessert spoon (dsp)" }
case object FluidOunce extends CookingUnit { val abbreviation = "floz"; val displayName = "Fluid Ounce (fl oz)" }
case object Gram extends CookingUnit { val abbreviation = "g"; val displayName = "Gram (g)" }
case object Grating extends CookingUnit { val abbreviation = "grating"; val displayName = "Grating" }
case object Handful extends CookingUnit { val abbreviation = "handful"; val displayName = "Handful" }
case object Head extends CookingUnit { val abbreviation = "head"; val displayName = "Head" }
case object Inch extends CookingUnit { val abbreviation = "inch"; val displayName = "Inch (in)" }
case object Kilogram extends CookingUnit { val abbreviation = "kg"; val displayName = "Kilogram (kg)" }
case object Knob extends CookingUnit { val abbreviation = "knob"; val displayName = "Knob" }
case object Litre extends CookingUnit { val abbreviation = "l"; val displayName = "Litre (l)" }
case object Millilitre extends CookingUnit { val abbreviation = "ml"; val displayName = "Millilitre (ml)" }
case object Millimetre extends CookingUnit { val abbreviation = "mm"; val displayName = "Millimetres (mm)" }
case object Ounce extends CookingUnit { val abbreviation = "oz"; val displayName = "Ounce (oz)" }
case object Packet extends CookingUnit { val abbreviation = "Packet"; val displayName = "Packet" }
case object Piece extends CookingUnit { val abbreviation = "piece"; val displayName = "Piece" }
case object Pinch extends CookingUnit { val abbreviation = "pinch"; val displayName = "Pinch" }
case object Pound extends CookingUnit { val abbreviation = "lb"; val displayName = "Pound (lb)" }
case object Sheet extends CookingUnit { val abbreviation = "sheet"; val displayName = "Sheet" }
case object Slice extends CookingUnit { val abbreviation = "slice"; val displayName = "Slice" }
case object Splash extends CookingUnit { val abbreviation = "splash"; val displayName = "Splash" }
case object Sprig extends CookingUnit { val abbreviation = "sprig"; val displayName = "Sprig" }
case object Stick extends CookingUnit { val abbreviation = "stick"; val displayName = "Stick" }
case object Tablespoon extends CookingUnit { val abbreviation = "tbsp"; val displayName = "Tablespoon (tbsp)" }
case object Teaspoon extends CookingUnit { val abbreviation = "tsp"; val displayName = "Teaspoon (tsp)" }

//if a new case object is added it must be added to UI select option list and unitMap above

case class Tags(list: Seq[Tag])

case class Tag(
  name: String,
  category: String
)

object Tag {
  val cuisines = Seq("African", "American", "Asian", "BBQ", "British", "Caribbean", "Chinese", "French", "Greek", "Indian", "Irish", "Italian", "Japanese", "Korean", "Lebanese", "Mediterranean", "Mexican", "Moroccan", "Nordic", "North African", "Persian", "Polish", "Portuguese", "South American", "Spanish", "Thai", "Turkish", "Vietnamese")
  val category = Seq("Baking", "Barbecue", "Breakfast", "Budget", "Canapes", "Dessert", "Dinner", "Dinner party", "Drinks and cocktails", "Healthy eating", "Lunch", "Main course", "Picnic", "Salads", "Sandwich", "Sides", "Soup", "Snacks", "Starters")
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
