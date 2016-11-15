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
  status: Status,
  steps: Steps
)

case class Steps(steps: Seq[String])

sealed trait Status

case object New extends Status
case object Pending extends Status
case object Curated extends Status
case object Verified extends Status
case object Finalised extends Status
case object Impossible extends Status

/*

`New` is brand new not looked at

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
  from: Int,
  to: Int
)

