package com.gu.recipeasy.models

import java.time.OffsetDateTime

case class Recipe(
  id: String,
  title: String,
  body: String,
  serves: Option[Serves],
  ingredientsLists: Seq[IngredientsList],
  articleId: String,
  credit: Option[String],
  publicationDate: OffsetDateTime,
  status: Status,
  steps: Seq[String]
)

sealed trait Status

case object New extends Status
case object Curated extends Status
case object Impossible extends Status

case class IngredientsList(
  title: Option[String],
  ingredients: Seq[String]
)

case class Serves(
  from: Int,
  to: Int
)
