package com.gu.recipeasy.models

import com.gu.recipeasy.db.DB
import org.jsoup.Jsoup
import org.jsoup.nodes.{ Element, Node, TextNode }

import scala.collection.JavaConverters._

object RecipeReadiness {

  def selectNewRecipesWithNonEmptyIngredientLists(db: DB): List[Recipe] = {
    db.getOriginalRecipes().filter(recipe => ((recipe.ingredientsLists.lists.size > 0) && (recipe.status == New)))
  }
  def migrateNewRecipesWithNonEmptyIngredientLists(db: DB): Int = {
    val recipes: List[Recipe] = selectNewRecipesWithNonEmptyIngredientLists(db)
    recipes.foreach(recipe => db.setOriginalRecipeStatus(recipe.id, Ready))
    recipes.size
  }

  def selectNigelSlaterRecipes(db: DB): List[Recipe] = {
    db.getOriginalRecipes().filter(_.status != Impossible).filter(_.credit.exists(_.contains("Nigel Slater")))
  }
  def migrateNigelSlaterRecipes(db: DB): Int = {
    val recipes: List[Recipe] = selectNigelSlaterRecipes(db).filter(_.status != Impossible)
    recipes.foreach(recipe => db.setOriginalRecipeStatus(recipe.id, Impossible))
    recipes.size
  }

  def isEmptyIngredientsLists(db: DB, recipe: Recipe): Boolean = {
    val maybeCuratedRecipe: Option[CuratedRecipeDB] = db.getCuratedRecipeByRecipeId(recipe.id)
    maybeCuratedRecipe.exists(curatedRecipe => curatedRecipe.ingredientsLists.lists.size > 0)
  }

  private object ShortListItem {
    private def isShort(length: Int) = length > 0 && length < 120
    def matches(node: Node): Boolean = node match {
      case tn: TextNode => isShort(tn.text.trim.size)
      case elem: Element if (elem.tag.getName == "strong" || elem.tag.getName == "em") => { isShort(elem.text.trim.size) }
      case other => false
    }
  }

  private object ParaWithListOfShortTexts {
    def matches(el: Element): Boolean = {
      if (el == null) false
      else {
        if (el.tag.getName == "p") {
          val pairs = el.childNodes.asScala.toList.grouped(2)
          pairs.forall {
            case x :: y :: Nil => ShortListItem.matches(x) && y.nodeName == "br"
            case x :: Nil => ShortListItem.matches(x) // final element
            case _ => false
          }
        } else false
      }
    }
    def unapply(el: Element): Option[Element] = if (matches(el)) Some(el) else None
  }
  def buildIngredientList(para: Element): Option[IngredientsList] = {
    def text(node: Node): String = node match {
      case tn: TextNode => tn.text.trim
      case elem: Element => elem.text.trim
      case other => ""
    }
    val listItems: Seq[Node] = para.childNodes.asScala.filterNot(_.nodeName == "br")
    val withoutServingCount = listItems.filterNot(n => text(n).toLowerCase.dropWhile(_ == '(').startsWith("serves"))
    if (withoutServingCount.size < 2) None
    else {
      if (withoutServingCount(0).nodeName != withoutServingCount(1).nodeName) {
        val title = text(withoutServingCount(0))
        val ingredients = withoutServingCount.drop(1).map(text)
        Some(IngredientsList(title = Some(title), ingredients))
      } else {
        val ingredients = withoutServingCount.map(text)
        Some(IngredientsList(title = None, ingredients))
      }
    }
  }
  def guessIngredients(body: Seq[Element]): Seq[IngredientsList] = {
    // Find paragraphs containing short text items separated by <br>
    val candidates = body.filter(ParaWithListOfShortTexts.matches)
    candidates.flatMap(buildIngredientList)
  }

  def extractIngredientsLists(recipe: Recipe): Seq[IngredientsList] = {
    val doc = Jsoup.parse(recipe.body)
    val body: Seq[Element] = doc.body.children.asScala
    guessIngredients(body)
  }

  def computeUpdatedCuratedRecipe(db: DB, recipe: Recipe, newIngredientsLists: Seq[IngredientsList]): Option[CuratedRecipe] = {
    val maybeCuratedRecipeOld: Option[CuratedRecipe] = db.getOriginalRecipe(recipe.id).map(CuratedRecipe.fromRecipe(_))
    maybeCuratedRecipeOld.map(curatedRecipeOld => {
      val detailedIngredientsList: Seq[DetailedIngredientsList] = newIngredientsLists.map { isl =>
        DetailedIngredientsList(
          isl.title,
          isl.ingredients.map(string => DetailedIngredient(
            quantity = Some(1.toDouble),
            unit = None,
            item = string,
            comment = None,
            raw = string
          ))
        )
      }
      val detailedIngredientsLists: DetailedIngredientsLists = DetailedIngredientsLists(detailedIngredientsList)
      curatedRecipeOld.copy(ingredientsLists = detailedIngredientsLists)
    })
  }

  def reparseNewRecipesWithEmptyIngredientLists(db: DB): Int = {
    val recipes: List[Recipe] = db.getOriginalRecipes()
      .filter(_.status == New)
      .filter(_.ingredientsLists.lists.size == 0) // selecting the ones with empty ingredient lists
      .filter(recipe => !isEmptyIngredientsLists(db, recipe)) // selecting the one which have not been acted on yet
      .filter(recipe => extractIngredientsLists(recipe).size > 0)

    recipes.foreach(recipe => {
      val newIngredientsLists = extractIngredientsLists(recipe)
      val maybeUpdatedCuratedRecipe: Option[CuratedRecipe] = computeUpdatedCuratedRecipe(db, recipe, newIngredientsLists)
      maybeUpdatedCuratedRecipe.foreach(updatedCuratedRecipe => {
        db.deleteCuratedRecipeByRecipeId(updatedCuratedRecipe.recipeId)
        db.insertCuratedRecipe(updatedCuratedRecipe)
      })
    })

    recipes.size
  }

  def updateRecipesReadiness(db: DB): Unit = {
    migrateNewRecipesWithNonEmptyIngredientLists(db: DB)
    migrateNigelSlaterRecipes(db: DB)
    reparseNewRecipesWithEmptyIngredientLists(db: DB)
  }

}
