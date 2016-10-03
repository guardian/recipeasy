package com.gu.recipeasy.models

//For simplicity CuratedRecipeDB has a reference to full tag
//CuratedRecipe fetches the Tag referenced by the ID from DB and stitches together
import java.time.OffsetDateTime

case class CuratedRecipeDB(
  id: Long,
  recipeId: String,
  title: String,
  serves: Option[Serves],
  ingredientsLists: DetailedIngredientsLists,
  credit: Option[String],
  times: TimesInMins,
  steps: Steps,
  tags: TagNames,
  images: Images
)

case class TagNames(list: Seq[String])

