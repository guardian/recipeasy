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

  def index = AuthAction { implicit request =>
    val curationIndex: Int = (db.curationCompletionRatio() * 100).toInt // expected to be an integer between 0 and 100
    val verificationIndex: Int = (db.verificationCompletionRatio() * 100).toInt // expected to be an integer between 0 and 100
    Ok(views.html.app("Recipeasy", curationIndex, verificationIndex))
  }

  // -------------------------------------------------------

  def viewRecipe(id: String) = AuthAction { implicit request =>
    val recipe = db.getOriginalRecipe(id)
    curatedRecipedEditor(recipe, editable = false, pageTopMessage = "")
  }

  private def isShowCreation(): Boolean = {
    val ParsingTime = db.countRecipesInGivenStatus(New) * 4
    val VerificationTime = db.countRecipesInGivenStatus(Curated) * 2 + db.countRecipesInGivenStatus(Verified)
    ParsingTime >= VerificationTime
  }

  def curateOrVerify() = AuthAction { implicit request =>
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

  def curateRecipe(id: String) = AuthAction { implicit request =>
    val recipe = db.getOriginalRecipe(id)
    curatedRecipedEditor(recipe, editable = true, pageTopMessage = "Creation | Pass 1/3")
  }

  def verifyRecipe(id: String) = AuthAction { implicit request =>
    val recipe = db.getOriginalRecipe(id)
    // We reuse the code for `curateRecipe` because curation and verification use the same logic and the same editor
    // But we need to record the fact that the recipe is being verified.
    curatedRecipedEditor(recipe, editable = true, pageTopMessage = "Verification | Pass 2/3")
  }

  def finalCheckRecipe(id: String) = AuthAction { implicit request =>
    val recipe = db.getOriginalRecipe(id)
    // We reuse the code for `curateRecipe` because curation and verification use the same logic and the same editor
    // But we need to record the fact that the recipe is being verified.
    curatedRecipedEditor(recipe, editable = true, pageTopMessage = "Final Check | Pass 3/3")
  }

  def curateOneRecipeInNewStatus = AuthAction { implicit request =>
    val newRecipe = db.getOriginalRecipeInNewStatus
    newRecipe match {
      case Some(r) => Redirect(routes.Application.curateRecipe(r.id))
      case None => NotFound
    }
  }

  private[this] def curatedRecipedEditor(
    recipe: Option[Recipe],
    editable: Boolean,
    pageTopMessage: String
  )(implicit req: RequestHeader) = {
    recipe match {
      case Some(r) => {

        /* if recipe has not being edited yet, mark as currently edited */
        if (r.status == New && editable) {
          db.setOriginalRecipeStatus(r.id, Pending)
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
          r.status
        ))

      }
      case None => NotFound
    }
  }

  // -------------------------------------------------------

  def postCuratedRecipe(recipeId: String) = AuthAction { implicit request =>
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
      db.moveStatusForward(recipeId)
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
        "preparationHours" -> optional(of[Int]),
        "preparationMinutes" -> optional(of[Int]),
        "cookingHours" -> optional(of[Int]),
        "cookingMinutes" -> optional(of[Int])
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

