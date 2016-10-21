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

  def index = AuthAction {
    Ok(views.html.app("Recipeasy"))
  }

  def curateNewRecipe = Action { implicit request =>
    val newRecipe = db.getNewRecipe
    newRecipe match {
      case Some(r) => Redirect(routes.Application.curateRecipe(r.id))
      case None => NotFound
    }
  }

  def curateRecipe(id: String) = Action { implicit request =>
    val recipe = db.getRecipe(id)
    view(recipe, editable = true)
  }

  def viewRecipe(id: String) = Action { implicit request =>
    val recipe = db.getRecipe(id)
    view(recipe, editable = false)
  }

  private[this] def view(recipe: Option[Recipe], editable: Boolean)(implicit req: RequestHeader) = {
    recipe match {
      case Some(r) => {

        /* if recipe has not being edited yet, mark as currently edited */
        if (r.status == New && editable) {
          db.setRecipeStatus(r.id, "Pending")
        }

        val curatedRecipe = db.getCuratedRecipeById(r.id).map(CuratedRecipe.fromCuratedRecipeDB) getOrElse CuratedRecipe.fromRecipe(r)
        val curatedRecipeForm = CuratedRecipeForm.toForm(curatedRecipe)
        val images = db.getImages(r.articleId)

        logger.info(s"View ${r.id}, ${r.title}")
        Ok(views.html.recipe(Application.curatedRecipeForm.fill(curatedRecipeForm), r.id, r.body, r.articleId, shouldShowButtons = editable, images))
      }
      case None => NotFound
    }
  }

  def postCuratedRecipe(recipeId: String) = Action { implicit request =>
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
      Redirect(routes.Application.curateNewRecipe)
    })
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

