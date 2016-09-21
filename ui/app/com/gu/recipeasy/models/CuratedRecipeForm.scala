package models

import com.gu.recipeasy.models._
import TagHelper._
import java.time.OffsetDateTime
import automagic._

case class CuratedRecipeForm(
  id: String,
  title: String,
  body: String,
  serves: Option[Serves],
  ingredientsLists: Seq[DetailedIngredientsList],
  articleId: String,
  credit: Option[String],
  publicationDate: String,
  status: String,
  times: TimesInMins,
  steps: Seq[String],
  tags: FormTags
)

object CuratedRecipeForm {

  def toForm(r: CuratedRecipe): CuratedRecipeForm = {
    transform[CuratedRecipe, CuratedRecipeForm](
      r,
      "ingredientsLists" -> r.ingredientsLists.lists,
      "publicationDate" -> r.publicationDate.toString,
      "status" -> r.status.toString,
      "steps" -> r.steps.steps,
      "tags" -> FormTags(r.tags)
    )
  }

  def fromForm(r: CuratedRecipeForm): CuratedRecipe = {
    val cuisineTags = getTags(r.tags.cuisine, "cuisine")
    val mealTypeTags = getTags(r.tags.mealType, "mealType")
    val holidayTags = getTags(r.tags.holiday, "holiday")
    val dietaryTags = getTags(r.tags.dietary, "dietary")

    transform[CuratedRecipeForm, CuratedRecipe](
      r,
      "ingredientsLists" -> DetailedIngredientsLists(r.ingredientsLists),
      "publicationDate" -> OffsetDateTime.parse(r.publicationDate),
      "status" -> Curated,
      "steps" -> Steps(r.steps),
      "tags" -> Tags(cuisineTags ++ mealTypeTags ++ holidayTags ++ dietaryTags)
    )
  }
}
