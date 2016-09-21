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
    Ok(views.html.app("Recipeasy"))
  }

  def curateRecipePage = Action { implicit request =>
    val newRecipe = db.getNewRecipe
    newRecipe match {
      case Some(r) => {
        val recipeId = r.id
        val body = r.body
        val articleId = r.articleId
        val curatedRecipe = CuratedRecipe.fromRecipe(r)
        val curatedRecipeForm = CuratedRecipeForm.toForm(curatedRecipe)
        db.setRecipeStatus(recipeId, "Pending")
        Ok(views.html.recipeLayout(createCuratedRecipeForm.fill(curatedRecipeForm), recipeId, body, articleId))
      }
      case None => NotFound
    }
  }

  def curateRecipe(recipeId: String) = Action { implicit request =>
    val formValidationResult = Application.createCuratedRecipeForm.bindFromRequest
    formValidationResult.fold({ formWithErrors =>
      val body = db.getBody(recipeId)
      val articleId = db.getArticleId(recipeId)
      BadRequest(views.html.recipeLayout(formWithErrors, recipeId, body, articleId))
    }, { r =>
      val halfBakedRecipe = fromForm(r)
      val recipeWithId = halfBakedRecipe.copy(recipeId = recipeId, id = 0L)
      db.insertCuratedRecipe(recipeWithId)
      db.setRecipeStatus(recipeId, "Curated")
      Redirect(routes.Application.curateRecipePage)
    })
  }
}

object Application {
  import models.TagHelper._
  import Forms._

  val createCuratedRecipeForm: Form[CuratedRecipeForm] = Form(
    mapping(
      "title" -> nonEmptyText(maxLength = 200),
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
      "credit" -> optional(text(maxLength = 200)),
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

