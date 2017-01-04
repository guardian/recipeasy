package models

import com.gu.recipeasy.models._
import TagHelper._
import automagic._

case class CuratedRecipeForm(
  title: String,
  serves: Option[DetailedServes],
  ingredientsLists: Seq[DetailedIngredientsList],
  credit: Option[String],
  times: TimesInMinsAdapted,
  steps: Seq[String],
  tags: FormTags,
  images: Seq[Image]
)

object CuratedRecipeForm {

  def toForm(r: CuratedRecipe): CuratedRecipeForm = {
    transform[CuratedRecipe, CuratedRecipeForm](
      r,
      "times" -> TimesInMinsAdapted.normalisedTimes(r),
      "ingredientsLists" -> r.ingredientsLists.lists,
      "steps" -> r.steps.steps,
      "tags" -> FormTags(r.tags),
      "images" -> r.images.images
    )
  }

  def fromForm(r: CuratedRecipeForm): CuratedRecipe = {

    val cuisineTags = getTags(r.tags.cuisine, "cuisine")
    val categoryTags = getTags(r.tags.category, "category")
    val holidayTags = getTags(r.tags.holiday, "holiday")
    val dietaryTags = getTags(r.tags.dietary, "dietary")

    transform[CuratedRecipeForm, CuratedRecipe](
      r,
      //these are set in the controller
      "id" -> 0L,
      "recipeId" -> "",
      "ingredientsLists" -> DetailedIngredientsLists(r.ingredientsLists),
      "steps" -> Steps(r.steps),
      "tags" -> Tags(cuisineTags ++ categoryTags ++ holidayTags ++ dietaryTags),
      "images" -> Images(r.images)
    )

  }

}
