package controllers

import com.gu.recipeasy.auth.AuthActions
import play.api.mvc._
import com.gu.recipeasy.views
import play.api.Configuration
import play.api.libs.ws.WSClient
import com.gu.recipeasy.db._
import com.gu.recipeasy.models._

class Application(override val wsClient: WSClient, override val conf: Configuration, db: DB) extends Controller with AuthActions {
  def index = AuthAction {
    val newRecipe = db.getNewRecipe
    newRecipe match {
      case Some(r) => Ok(views.html.app(recipeTypeConversion.convertToCurated(r)))
      case None => NotFound
    }
  }
}

object recipeTypeConversion {
  def rawToDetailedIngredientsLists(ingredients: IngredientsLists): DetailedIngredientsLists = {
    new DetailedIngredientsLists(lists =
      ingredients.lists.map(r => new DetailedIngredientsList(r.title, rawToDetailedIngredients(r.ingredients))))
  }

  def rawToDetailedIngredients(ingredients: Seq[String]): Seq[DetailedIngredient] = {
    ingredients.map(i => new DetailedIngredient(None, None, None, None, i))
  }

  def convertToCurated(r: Recipe): CuratedRecipe = {
    new CuratedRecipe(
      id = r.id,
      title = r.title,
      serves = r.serves,
      ingredientsLists = rawToDetailedIngredientsLists(r.ingredientsLists),
      articleId = r.articleId,
      credit = r.credit,
      publicationDate = r.publicationDate,
      status = r.status,
      steps = r.steps,
      times = None,
      tags = None
    )
  }
}
