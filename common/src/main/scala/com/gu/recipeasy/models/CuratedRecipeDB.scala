package com.gu.recipeasy.models

//this model is different from CuratedRecipe as it holds a list of Tag ids
//whereas CuratedRecipe has fetches the Tag referenced by the ID from DB
import java.time.OffsetDateTime

case class CuratedRecipeDB(
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
  tags: Option[TagNames]
)

case class TagNames(list: Seq[String])

