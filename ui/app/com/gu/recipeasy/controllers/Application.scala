package controllers

import java.time.OffsetDateTime

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

  // -------------------------------------------------------

  def viewRecipe(id: String) = Action { implicit request =>
    val recipe = db.getOriginalRecipe(id)
    curatedRecipedEditor(recipe, editable = false, pageTopMessage = "", showSkipThisRecipeButton = false)
  }

  private def isShowCreation(): Boolean = {
    // TODO: write the logic to compute the correct answer here
    true
  }

  def curateOrVerify() = Action { implicit request =>
    if (isShowCreation()) {
      val newRecipe = db.getOriginalRecipeInNewStatus
      newRecipe match {
        case Some(recipe) => Redirect(routes.Application.curateRecipe(recipe.id))
        case None => NotFound
      }
    } else {
      val maybeRecipe = db.getCuratedRecipe()
      maybeRecipe match {
        case Some(recipe) => Redirect(routes.Application.verifyRecipe(recipe.recipeId))
        case None => NotFound
      }
    }
  }

  def curateRecipe(id: String) = Action { implicit request =>
    val recipe = db.getOriginalRecipe(id)
    curatedRecipedEditor(recipe, editable = true, pageTopMessage = "Creation | Pass 1/3", showSkipThisRecipeButton = true)
  }

  def verifyRecipe(id: String) = Action { implicit request =>
    val recipe = db.getOriginalRecipe(id)
    // We reuse the code for `curateRecipe` because curation and verification use the same logic and the same editor
    // But we need to record the fact that the recipe is being verified.
    curatedRecipedEditor(recipe, editable = true, pageTopMessage = "Verification | Pass 2/3", showSkipThisRecipeButton = false)
  }

  def finalCheckRecipe(id: String) = Action { implicit request =>
    val recipe = db.getOriginalRecipe(id)
    // We reuse the code for `curateRecipe` because curation and verification use the same logic and the same editor
    // But we need to record the fact that the recipe is being verified.
    curatedRecipedEditor(recipe, editable = true, pageTopMessage = "Final Check | Pass 3/3", showSkipThisRecipeButton = false)
  }

  def curateOneRecipeInNewStatus = Action { implicit request =>
    val newRecipe = db.getOriginalRecipeInNewStatus
    newRecipe match {
      case Some(r) => Redirect(routes.Application.curateRecipe(r.id))
      case None => NotFound
    }
  }

  private[this] def curatedRecipedEditor(
    recipe: Option[Recipe],
    editable: Boolean,
    pageTopMessage: String,
    showSkipThisRecipeButton: Boolean
  )(implicit req: RequestHeader) = {
    recipe match {
      case Some(r) => {

        /* if recipe has not being edited yet, mark as currently edited */
        if (r.status == New && editable) {
          db.setOriginalRecipeStatus(r.id, "Pending")
        }

        val curatedRecipe = db.getCuratedRecipeByRecipeId(r.id).map(CuratedRecipe.fromCuratedRecipeDB) getOrElse CuratedRecipe.fromRecipe(r)
        val curatedRecipeForm = CuratedRecipeForm.toForm(curatedRecipe)
        val images = db.getImages(r.articleId)

        logger.info(s"View ${r.id}, ${r.title}")
        Ok(views.html.recipe(
          Application.curatedRecipeForm.fill(curatedRecipeForm),
          r.id,
          r.body,
          r.articleId,
          shouldShowButtons = editable,
          images,
          pageTopMessage,
          showSkipThisRecipeButton
        ))

      }
      case None => NotFound
    }
  }

  // -------------------------------------------------------

  def postCuratedRecipe(recipeId: String) = Action { implicit request =>
    val formValidationResult = Application.curatedRecipeForm.bindFromRequest
    formValidationResult.fold({ formWithErrors =>
      val originalRecipe = db.getOriginalRecipe(recipeId)
      originalRecipe match {
        case Some(recipe) => {
          logger.debug(s"Incorrect form submission $recipeId")
          BadRequest(views.html.error("Recipeasy", "Incorrect form submission"))
        }
        case None => NotFound
      }
    }, { r =>
      val curatedRecipeWithoutId = fromForm(r)
      val curatedRecipeWithId = curatedRecipeWithoutId.copy(recipeId = recipeId, id = 0L)
      db.deleteCuratedRecipeByRecipeId(recipeId)
      db.insertCuratedRecipe(curatedRecipeWithId)
      db.setOriginalRecipeStatus(recipeId, "Curated")
      Redirect(routes.Application.curateOneRecipeInNewStatus)
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
        "preparationHours" -> optional(of[Double]),
        "preparationMinutes" -> optional(of[Double]),
        "cookingHours" -> optional(of[Double]),
        "cookingMinutes" -> optional(of[Double])
      )(TimesInMinsAdapted.apply)(TimesInMinsAdapted.unapply),
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

