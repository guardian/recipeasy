package com.gu.recipeasy.models

import automagic._
import io.circe._
import cats.data.Xor
import CuratedRecipeDB._
import ImageDB._

import com.gu.contentatom.thrift.atom.{ recipe => atom }
import com.gu.contentatom.thrift._

case class CuratedRecipe(
  id: Long,
  recipeId: String,
  title: String,
  serves: Option[DetailedServes],
  ingredientsLists: DetailedIngredientsLists,
  credit: Option[String],
  times: TimesInMinsAdapted,
  steps: Steps,
  tags: Tags,
  images: Images
)

object CuratedRecipe {

  def fromRecipe(r: Recipe): CuratedRecipe = {
    transform[Recipe, CuratedRecipe](
      r,
      "id" -> 0L,
      "recipeId" -> r.id,
      "serves" -> DetailedServes.fromServes(r.serves),
      "times" -> TimesInMinsAdapted(None, None, None, None),
      "tags" -> Tags(List.empty),
      "ingredientsLists" -> DetailedIngredientsLists.fromIngredientsLists(r.ingredientsLists),
      "images" -> Images(List.empty)
    )
  }

  def toDBModel(cr: CuratedRecipe): CuratedRecipeDB = {

    transform[CuratedRecipe, CuratedRecipeDB](
      cr,
      "tags" -> getTagNames(cr.tags),
      "times" -> TimesInMins(
        Some(TimesInMinsAdapted.preparationTimeInMinutes(cr)),
        Some(TimesInMinsAdapted.cookingTimeInMinutes(cr))
      )
    )

  }

  def fromCuratedRecipeDB(r: CuratedRecipeDB): CuratedRecipe = {
    transform[CuratedRecipeDB, CuratedRecipe](
      r,
      "tags" -> getFullTags(r.tags),
      "times" -> TimesInMinsAdapted(None, r.times.preparation.map(t => t.toInt), None, r.times.cooking.map(t => t.toInt))
    )
  }

  def getTagNames(tags: Tags): TagNames = {
    TagNames(tags.list.map(t => t.name))
  }

  def getFullTags(tags: TagNames): Tags = {
    Tags(tags.list.collect {
      case t if Tag.cuisines.contains(t) => Tag(t, "cuisines")
      case t if Tag.category.contains(t) => Tag(t, "category")
      case t if Tag.holidays.contains(t) => Tag(t, "holidays")
      case t if Tag.dietary.contains(t) => Tag(t, "dietary")
    })
  }

  def toAtom(r: Recipe, cr: CuratedRecipe): Atom = {

    val contentChangeDetails = ContentChangeDetails(
      created = Some(
        ChangeRecord(
          date = r.publicationDate.toInstant.toEpochMilli,
          user = Some(
            User(email = "off-platform@guardian.co.uk")
          )
        )
      ),
      published = Some(
        ChangeRecord(
          date = r.publicationDate.toInstant.toEpochMilli,
          user = Some(
            User(email = "off-platform@guardian.co.uk")
          )
        )
      ),
      revision = 1L
    )

    val recipeAtom = atom.RecipeAtom(
      title = cr.title,
      tags = atom.Tags(
        cuisine = cr.tags.list.collect { case Tag(value, "cuisines") => value },
        category = cr.tags.list.collect { case Tag(value, "category") => value },
        celebration = cr.tags.list.collect { case Tag(value, "holidays") => value },
        dietary = cr.tags.list.collect { case Tag(value, "dietary") => value }
      ),
      time = atom.Time(
        preparation = {
        val t = TimesInMinsAdapted.preparationTimeInMinutes(cr)
        if (t == 0) None else Some(t.toShort)
      },
        cooking = {
        val t = TimesInMinsAdapted.cookingTimeInMinutes(cr)
        if (t == 0) None else Some(t.toShort)
      }
      ),
      serves = cr.serves.map(s => atom.Serves(
        `type` = s.portion match {
          case MakesType => "makes"
          case ServesType => "serves"
          case QuantityType => "quantity"
        },
        from = s.quantity.from.toShort,
        to = s.quantity.to.toShort,
        unit = s.unit
      )),
      ingredientsLists = cr.ingredientsLists.lists.map(ingredientsList =>
        atom.IngredientsList(
          ingredientsList.title,
          ingredientsList.ingredients.map(ingredient =>
            atom.Ingredient(
              item = ingredient.item,
              comment = ingredient.comment,
              quantity = ingredient.quantity,
              unit = ingredient.unit.map(_.abbreviation)
            ))
        )),
      steps = cr.steps.steps,
      credits = cr.credit.toList,
      images = Nil
    )

    Atom(
      id = r.id,
      atomType = AtomType.Recipe,
      labels = Seq.empty,
      defaultHtml = "",
      data = AtomData.Recipe(recipeAtom),
      contentChangeDetails = contentChangeDetails
    )
  }

}

