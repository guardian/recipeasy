package com.gu.recipeasy.models

import automagic._
import com.gu.contentatom.thrift.{ Atom, AtomData, AtomType, ChangeRecord, ContentChangeDetails, User, Image => AtomImage }
import com.gu.contentatom.thrift.atom.{ recipe => atom }

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

  def atomRangeFromOptionalBounds(maybeFrom: Option[Double], maybeTo: Option[Double]): Option[atom.Range] = {
    for {
      from <- maybeFrom
      to <- maybeTo
    } yield {
      atom.Range((from * 100).toShort, (to * 100).toShort)
    }
  }

  def atomServeQuantity(portion: PortionType, quantity: Double, maybeUnit: Option[String]): Short = {
    // If the PortionType is QuantityType and the unit is "kilograms" or "litres", then we multiply the quantity by 1000
    // Note: the unit will be translated to "grams" or "millilitres" in atomServeUnit
    portion match {
      case MakesType => quantity.toShort
      case ServesType => quantity.toShort
      case QuantityType => {
        maybeUnit match {
          case Some(unit) => {
            unit match {
              case PortionUnitKilograms.name => (quantity * 1000).toShort
              case PortionUnitLitres.name => (quantity * 1000).toShort
              case _ => quantity.toShort
            }
          }
          case None => 0.toShort
        }
      }
    }
  }

  def atomServeUnit(portion: PortionType, maybeUnit: Option[String]): Option[String] = {
    // If the PortionType is QuantityType and the unit is "kilograms" or "litres", then the unit is translated to "grams" or "millilitres"
    // Note: the quantity is multiplied by 1000 in atomServeQuantity
    portion match {
      case QuantityType => {
        maybeUnit match {
          case Some(unit) => {
            unit match {
              case PortionUnitKilograms.name => Some(PortionUnitGrams.name)
              case PortionUnitLitres.name => Some(PortionUnitMillilitres.name)
              case _ => Some(unit)
            }
          }
          case _ => None
        }
      }
      case _ => maybeUnit
    }
  }

  def toAtom(r: Recipe, cr: CuratedRecipe, curatedRecipeImages: Seq[AtomImage]): Atom = {

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
        cuisine = cr.tags.list.collect { case Tag(value, "cuisines") => value.toLowerCase },
        category = cr.tags.list.collect { case Tag(value, "category") => value.toLowerCase },
        celebration = cr.tags.list.collect { case Tag(value, "holidays") => value.toLowerCase },
        dietary = cr.tags.list.collect { case Tag(value, "dietary") => value.toLowerCase }
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
        from = atomServeQuantity(s.portion, s.quantity.from, s.unit),
        to = atomServeQuantity(s.portion, s.quantity.to, s.unit),
        unit = atomServeUnit(s.portion, s.unit)
      )),
      ingredientsLists = cr.ingredientsLists.lists.map(ingredientsList =>
        atom.IngredientsList(
          ingredientsList.title,
          ingredientsList.ingredients.map(ingredient =>
            atom.Ingredient(
              item = ingredient.item,
              comment = ingredient.comment,
              quantity = ingredient.quantity,
              quantityRange = atomRangeFromOptionalBounds(ingredient.quantityRangeFrom, ingredient.quantityRangeTo), // We store the range quantities*100 to preserve two decimals of a Double in a situation we need to provide Ints
              unit = ingredient.unit.map(_.abbreviation)
            ))
        )),
      steps = cr.steps.steps,
      credits = cr.credit.toList,
      images = curatedRecipeImages,
      sourceArticleId = Some(r.articleId)
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

