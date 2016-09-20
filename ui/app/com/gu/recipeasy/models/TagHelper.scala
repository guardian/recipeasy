package models

import com.gu.recipeasy.models._

object TagHelper {

  case class FormTags(
    cuisine: Seq[String],
    mealType: Seq[String],
    holiday: Seq[String],
    dietary: Dietary
  )

  object FormTags {
    def apply(tags: Tags): FormTags = {
      FormTags(
        cuisine = tags.list.collect { case t if t.category == "cuisine" => t.name },
        mealType = tags.list.collect { case t if t.category == "mealType" => t.name },
        holiday = tags.list.collect { case t if t.category == "holiday" => t.name },
        dietary = Dietary(tags)
      )
    }
  }

  case class Dietary(
    lowSugar: Boolean,
    lowFat: Boolean,
    highFibre: Boolean,
    nutFree: Boolean,
    glutenFree: Boolean,
    dairyFree: Boolean,
    eggFree: Boolean,
    vegetarian: Boolean,
    vegan: Boolean
  )

  object Dietary {
    def apply(tags: Tags): Dietary = {
      Dietary(
        lowSugar = tags.list.contains(Tag.lowSugar),
        lowFat = tags.list.contains(Tag.lowFat),
        highFibre = tags.list.contains(Tag.highFibre),
        nutFree = tags.list.contains(Tag.nutFree),
        glutenFree = tags.list.contains(Tag.glutenFree),
        dairyFree = tags.list.contains(Tag.dairyFree),
        eggFree = tags.list.contains(Tag.eggFree),
        vegetarian = tags.list.contains(Tag.vegetarian),
        vegan = tags.list.contains(Tag.vegan)
      )
    }
  }

  def getTags(tags: Seq[String], cat: String): Seq[Tag] = {
    tags.collect { case s: String if (!s.isEmpty) => Tag(s, cat) }
  }

  //TODO make nicer
  def getDietaryTags(t: Dietary): Seq[Tag] = {
    val tags = Map(
      Tag.lowSugar -> t.lowSugar,
      Tag.lowFat -> t.lowFat,
      Tag.highFibre -> t.highFibre,
      Tag.nutFree -> t.nutFree,
      Tag.glutenFree -> t.glutenFree,
      Tag.dairyFree -> t.dairyFree,
      Tag.eggFree -> t.eggFree,
      Tag.vegetarian -> t.vegetarian,
      Tag.vegan -> t.vegan
    )

    tags.filter { _._2 }.keySet.toSeq
  }
}

