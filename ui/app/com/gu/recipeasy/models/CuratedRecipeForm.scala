package models

import com.gu.recipeasy.models._
import TagHelper._
import automagic._

case class CuratedRecipeForm(
  title: String,
  serves: Option[Serves],
  ingredientsLists: Seq[DetailedIngredientsList],
  credit: Option[String],
  times: TimesInMins,
  steps: Seq[String],
  tags: FormTags
)

object CuratedRecipeForm {

  def toForm(r: CuratedRecipe): CuratedRecipeForm = {
    transform[CuratedRecipe, CuratedRecipeForm](
      r,
      "ingredientsLists" -> r.ingredientsLists.lists,
      "steps" -> r.steps.steps,
      "tags" -> FormTags(r.tags)
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
      "tags" -> Tags(cuisineTags ++ categoryTags ++ holidayTags ++ dietaryTags)
    )
  }
}
