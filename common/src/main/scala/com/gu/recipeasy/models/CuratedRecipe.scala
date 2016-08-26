package com.gu.recipeasy.models

import java.time.OffsetDateTime

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
  times: Option[Times],
  steps: Option[Steps],
  tags: Option[Tags]
)

case class DetailedIngredientsLists(lists: Seq[DetailedIngredientsList])

case class DetailedIngredientsList(
  title: Option[String],
  ingredients: Seq[DetailedIngredient]
)

case class DetailedIngredient(
  quantity: Option[Double],
  unit: Option[CookingUnit],
  item: Option[String],
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

sealed trait CookingUnit

case object Gram extends CookingUnit
case object Milliltre extends CookingUnit
case object Litre extends CookingUnit
case object Ounces extends CookingUnit
case object FluidOunces extends CookingUnit
case object Cup extends CookingUnit
case object Teaspoon extends CookingUnit
case object Tablespoon extends CookingUnit
case object Pinch extends CookingUnit
case object Handful extends CookingUnit
case object Grating extends CookingUnit

case class Tags(list: Seq[Tag])

case class Tag(
  name: String,
  category: String
)
