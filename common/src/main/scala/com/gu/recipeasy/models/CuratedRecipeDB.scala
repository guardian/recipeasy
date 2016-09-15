package com.gu.recipeasy.models

//For simplicity CuratedRecipeDB has a reference to full tag
//CuratedRecipe fetches the Tag referenced by the ID from DB and stitches together
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
  times: TimesInMins,
  steps: Steps,
  tags: TagNames
)

case class TagNames(list: Seq[String])

