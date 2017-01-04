package com.gu.recipeasy.models

import java.time.OffsetDateTime

case class Recipe(
  id: String,
  title: String,
  body: String,
  serves: Option[Serves],
  ingredientsLists: IngredientsLists,
  articleId: String,
  credit: Option[String],
  publicationDate: OffsetDateTime,
  status: RecipeStatus,
  steps: Steps
)

case class Steps(steps: Seq[String])

sealed trait RecipeStatus

case object New extends RecipeStatus
case object Ready extends RecipeStatus
case object Pending extends RecipeStatus
case object Curated extends RecipeStatus
case object Verified extends RecipeStatus
case object Finalised extends RecipeStatus
case object Impossible extends RecipeStatus

/*

`New` is brand new

`Ready` means that the recipe's ingredient lists have been correctly parsed
    This appeared because initially not all `New` recipes had been correctly parsed

`Pending` means it is currently being looked at
    This is used to prevent two `New` recipes to be curated by two people at the same time
    So only `New` (non `Pending` recipes are being displayed in the original curation/parsing process)

`Curated` means it has been parsed (and therefore there is a record in the curated_recipe table)

`Verified` that it has been verified once
`Finalised` that it has been verified twice

 */

case class IngredientsLists(lists: Seq[IngredientsList])

case class IngredientsList(
  title: Option[String],
  ingredients: Seq[String]
)

case class Serves(
  from: Double,
  to: Double
)

