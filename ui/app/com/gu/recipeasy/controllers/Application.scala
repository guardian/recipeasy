package controllers

import com.gu.recipeasy.auth.AuthActions
import play.api.mvc._
import com.gu.recipeasy.views
import play.api.Configuration
import play.api.libs.ws.WSClient
import com.gu.recipeasy.db._
import com.gu.recipeasy.models._
import automagic._

class Application(override val wsClient: WSClient, override val conf: Configuration, db: DB) extends Controller with AuthActions {
  def index = AuthAction {
    val newRecipe = db.getNewRecipe
    newRecipe match {
      case Some(r) => Ok(views.html.app(recipeTypeConversion.transformRecipe(r)))
      case None => NotFound
    }
  }
}

object recipeTypeConversion {
  def transformRecipe(r: Recipe): CuratedRecipe = {
    transform[Recipe, CuratedRecipe](
      r,
      "times" -> None,
      "tags" -> None,
      "ingredientsLists" -> rawToDetailedIngredientsLists(r.ingredientsLists)
    )
  }

  def rawToDetailedIngredientsLists(ingredients: IngredientsLists): DetailedIngredientsLists = {
    new DetailedIngredientsLists(lists =
      ingredients.lists.map(r => new DetailedIngredientsList(r.title, rawToDetailedIngredients(r.ingredients))))
  }

  def rawToDetailedIngredients(ingredients: Seq[String]): Seq[DetailedIngredient] = {
    ingredients.map(i => new DetailedIngredient(None, None, None, None, i))
  }

}
