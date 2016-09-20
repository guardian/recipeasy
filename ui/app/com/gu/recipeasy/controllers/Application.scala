package controllers

import play.api.mvc._
import play.api.Configuration
import play.api.libs.ws.WSClient
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.i18n.{ I18nSupport, MessagesApi }
import java.time.OffsetDateTime

import com.gu.recipeasy.auth.AuthActions
import com.gu.recipeasy.db._
import com.gu.recipeasy.models._
import com.gu.recipeasy.views
import models._
import models.CuratedRecipeForm._
import automagic._

class Application(override val wsClient: WSClient, override val conf: Configuration, db: DB, val messagesApi: MessagesApi) extends Controller with AuthActions with I18nSupport {
  import Forms._
  import Application._

  def index = AuthAction {
    Ok(views.html.app(("Title")))
  }

  def curateRecipePage = Action { implicit request =>
    val newRecipe = db.getNewRecipe
    newRecipe match {
      case Some(r) => {
        db.setRecipeStatus(r.id, "Pending")
        Ok(views.html.recipeLayout(createCuratedRecipeForm.fill(toForm(recipeTypeConversion.transformRecipe(r)))))
      }
      case None => NotFound
    }
  }

  def curateRecipe = Action { implicit request =>
    //create curated recipe to store in db
    val formValidationResult = Application.createCuratedRecipeForm.bindFromRequest
    formValidationResult.fold({ formWithErrors =>
      BadRequest(views.html.recipeLayout(formWithErrors))
    }, { r =>
      val recipe = fromForm(r)
      db.insertCuratedRecipe(recipe)
      db.setRecipeStatus(r.id, "Curated")
      Redirect(routes.Application.curateRecipePage)
    })
  }

}

object recipeTypeConversion {
  def transformRecipe(r: Recipe): CuratedRecipe = {
    transform[Recipe, CuratedRecipe](
      r,
      "times" -> TimesInMins(None, None),
      "tags" -> Tags(List.empty),
      "ingredientsLists" -> rawToDetailedIngredientsLists(r.ingredientsLists)
    )
  }

  def rawToDetailedIngredientsLists(ingredients: IngredientsLists): DetailedIngredientsLists = {
    new DetailedIngredientsLists(lists =
      ingredients.lists.map(r => new DetailedIngredientsList(r.title, rawToDetailedIngredients(r.ingredients))))
  }

  def rawToDetailedIngredients(ingredients: Seq[String]): Seq[DetailedIngredient] = {
    ingredients.map(i => new DetailedIngredient(None, None, "", None, i))
  }

}

object Application {
  import models.TagHelper._
  import Forms._

  val createCuratedRecipeForm: Form[CuratedRecipeForm] = Form(
    mapping(
      "id" -> of[String],
      "title" -> nonEmptyText(maxLength = 200),
      "body" -> of[String],
      "serves" -> optional(mapping(
        "from" -> number(min = 1),
        "to" -> number(min = 1)
      )(Serves.apply)(Serves.unapply)),
      "ingredientsLists" -> seq(mapping(
        "title" -> optional(nonEmptyText(maxLength = 200)),
        "ingredients" -> seq(mapping(
          "quantity" -> optional(of[Double]),
          "unit" -> optional(text.transform[CookingUnit](CookingUnit.fromString(_).getOrElse(Handful), _.abbreviation)),
          "item" -> nonEmptyText(maxLength = 200),
          "comment" -> optional(text(maxLength = 200)),
          "raw" -> text
        )(DetailedIngredient.apply)(DetailedIngredient.unapply))
      )(DetailedIngredientsList.apply)(DetailedIngredientsList.unapply)),
      "articleId" -> of[String],
      "credit" -> optional(text(maxLength = 200)),
      "publicationDate" -> of[String],
      "status" -> of[String],
      "times" -> mapping(
        "preparation" -> optional(of[Double]),
        "cooking" -> optional(of[Double])
      )(TimesInMins.apply)(TimesInMins.unapply),
      "steps" -> seq(text),
      "tags" -> mapping(
        "cuisine" -> seq(text),
        "mealType" -> seq(text),
        "holiday" -> seq(text),
        "dietary" -> seq(text)
      )(FormTags.apply)(FormTags.unapply)
    )(CuratedRecipeForm.apply)(CuratedRecipeForm.unapply)
  )
}

