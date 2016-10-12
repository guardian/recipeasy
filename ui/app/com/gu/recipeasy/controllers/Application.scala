package controllers

import play.api.mvc._
import play.api.Configuration
import play.api.libs.ws.WSClient
import play.api.data._
import play.api.data.format.Formats._
import play.api.i18n.{ I18nSupport, MessagesApi }
import com.typesafe.scalalogging.StrictLogging

import com.gu.recipeasy.auth.AuthActions
import com.gu.recipeasy.db._
import com.gu.recipeasy.models._
import com.gu.recipeasy.views
import models._
import models.CuratedRecipeForm._

class Application(override val wsClient: WSClient, override val conf: Configuration, db: DB, val messagesApi: MessagesApi) extends Controller with AuthActions with I18nSupport with StrictLogging {
  import Application._

  def index = AuthAction {
    Ok(views.html.app("Recipeasy"))
  }

  def curateRecipePage = Action { implicit request =>
    val newRecipe = db.getNewRecipe
    newRecipe match {
      case Some(r) => {
        val curatedRecipe = CuratedRecipe.fromRecipe(r)
        val curatedRecipeForm = CuratedRecipeForm.toForm(curatedRecipe)
        val images = db.getImages(r.articleId)
        db.setRecipeStatus(r.id, "Pending")
        Ok(views.html.recipe(Application.curatedRecipeForm.fill(curatedRecipeForm), r.id, r.body, r.articleId, shouldShowButtons = true, images))
      }
      case None => NotFound
    }
  }

  def curateRecipe(recipeId: String) = Action { implicit request =>
    val formValidationResult = Application.curatedRecipeForm.bindFromRequest
    formValidationResult.fold({ formWithErrors =>
      val originalRecipe = db.getRecipe(recipeId)
      originalRecipe match {
        case Some(recipe) => {
          logger.debug(s"Could not look up recipe using $recipeId")
          BadRequest(views.html.error("Recipeasy", "Could not find recipe"))
        }
        case None => NotFound
      }
    }, { r =>
      val halfBakedRecipe = fromForm(r)
      val recipeWithId = halfBakedRecipe.copy(recipeId = recipeId, id = 0L)
      db.insertCuratedRecipe(recipeWithId)
      db.setRecipeStatus(recipeId, "Curated")
      Redirect(routes.Application.curateRecipePage)
    })
  }

  def viewRecipe(recipeId: String) = Action { implicit request =>
    val newRecipe = db.getRecipe(recipeId)
    newRecipe match {
      case Some(r) => {
        val curatedRecipe = CuratedRecipe.fromRecipe(r)
        val curatedRecipeForm = CuratedRecipeForm.toForm(curatedRecipe)
        val images = db.getImages(r.articleId)
        db.setRecipeStatus(r.id, "Pending")
        Ok(views.html.recipe(Application.curatedRecipeForm.fill(curatedRecipeForm), r.id, r.body, r.articleId, shouldShowButtons = false, images))
      }
      case None => NotFound
    }
  }

}

object Application {
  import models.TagHelper._
  import Forms._

  val curatedRecipeForm: Form[CuratedRecipeForm] = Form(
    mapping(
      "title" -> nonEmptyText(maxLength = 200),
      "serves" -> optional(mapping(
        "portionType" -> text.transform[PortionType](PortionType.fromString(_), _.toString),
        "quantity" -> mapping(
          "from" -> number(min = 1),
          "to" -> number(min = 1)
        )(Serves.apply)(Serves.unapply)
      )(DetailedServes.apply)(DetailedServes.unapply)),
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
      "credit" -> optional(text(maxLength = 200)),
      "times" -> mapping(
        "preparation" -> optional(of[Double]),
        "cooking" -> optional(of[Double])
      )(TimesInMins.apply)(TimesInMins.unapply),
      "steps" -> seq(text),
      "tags" -> mapping(
        "cuisine" -> seq(text),
        "category" -> seq(text),
        "holiday" -> seq(text),
        "dietary" -> seq(text)
      )(FormTags.apply)(FormTags.unapply),
      "images" -> seq(mapping(
        "mediaId" -> (text),
        "assetUrl" -> (text),
        "altText" -> (text)
      )(Image.apply)(Image.unapply))
    )(CuratedRecipeForm.apply)(CuratedRecipeForm.unapply)
  )
}

