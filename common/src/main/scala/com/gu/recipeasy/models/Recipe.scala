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

sealed trait RecipeStatus { val name: String }

// Changes in this trait, should be reported to DB.scala's private implicit val decodeStatus

case object RecipeStatusNew extends RecipeStatus { val name = "New" }
case object RecipeStatusReady extends RecipeStatus { val name = "Ready" }
case object RecipeStatusCurated extends RecipeStatus { val name = "Curated" }
case object RecipeStatusVerified extends RecipeStatus { val name = "Verified" }
case object RecipeStatusFinalised extends RecipeStatus { val name = "Finalised" }
case object RecipeStatusImpossible extends RecipeStatus { val name = "Impossible" }

case object RecipeStatusPendingCuration extends RecipeStatus { val name = "Pending" }
case object RecipeStatusPendingVerification extends RecipeStatus { val name = "PendingVerification" }
case object RecipeStatusPendingFinalisation extends RecipeStatus { val name = "PendingFinalisation" }

/*

`New` is brand new

`Ready` means that the recipe's ingredient lists have been correctly parsed.
    Introduced because initially not all `New` recipes had been correctly parsed.

`Curated` means it has been parsed (and therefore there is a record in the curated_recipe table).

`Verified` that it has been verified once.
`Finalised` that it has been verified twice.


All the pending states...
  RecipeStatusPendingCuration
  RecipeStatusPendingVerification
  RecipeStatusPendingFinalisation
... are used to prevent two users from operating on the same recipe.

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

