package controllers

import play.api.mvc._
import play.api.Configuration
import play.api.libs.ws.WSClient
import play.api.data._
import play.api.data.format.Formats._
import play.api.i18n.{I18nSupport, MessagesApi}
import com.typesafe.scalalogging.StrictLogging
import com.gu.recipeasy.auth.AuthActions
import com.gu.recipeasy.db._
import com.gu.recipeasy.models._
<<<<<<< HEAD
import com.gu.recipeasy.services.RecipePublisher
=======
import com.gu.recipeasy.services.ContentApi
import services.{ PublisherConfig, RecipePublisher, Teleporter }
>>>>>>> 39d4fb100da8f4a39028cd184f3d8386ca80a2a5
import com.gu.recipeasy.views
import models._
import models.CuratedRecipeForm._

class Application(override val wsClient: WSClient, override val conf: Configuration, db: DB, val messagesApi: MessagesApi, publisherConfig: PublisherConfig, teleporter: Teleporter, contentApi: ContentApi) extends Controller with AuthActions with I18nSupport with StrictLogging {

  def index = AuthAction { implicit request =>
    val progressBarPercentage: Double = (db.progressBarRatio() * 10000).toInt.toDouble / 100
    val generalStats = db.generalStats()
    val userStats = db.userStats(request.user.email)
    Ok(views.html.app("Recipeasy", request.user.firstName, progressBarPercentage, generalStats, userStats))
  }

  def tutorial() = AuthAction { implicit request =>
    Ok(views.html.tutorial())
  }

  // -------------------------------------------------------

  def viewRecipe(id: String) = AuthAction { implicit request =>
    val recipe = db.getOriginalRecipe(id)
    db.insertUserEvent(UserEvent(request.user.email, request.user.firstName, request.user.lastName, id, UserEventAccessRecipeReadOnlyPage.name))
    curatedRecipedEditor(recipe, editable = false, curationUser = CurationUser.getCurationUser(id, db))
  }

  def curateOrVerify() = AuthAction { implicit request =>
    val maybeRecipe = db.getUserSpecificRecipeForVerificationStep(request.user.email)
    maybeRecipe match {
      case Some(recipe) => Redirect(routes.Application.verifyRecipe(recipe.id))
      case None =>
        db.getOriginalRecipeInReadyStatus() match {
          case Some(recipe) => Redirect(routes.Application.curateRecipe(recipe.id))
          case None => NotFound
        }
    }
  }

  def curateRecipe(id: String) = AuthAction { implicit request =>
    db.getOriginalRecipe(id) match {
      case None => NotFound
      case Some(recipe) =>
        val isCuratable = recipe.status == RecipeStatusReady || recipe.status == RecipeStatusPendingCuration
        if (isCuratable) {
          db.moveRecipeStatusFromStableStateToNextPendingState(id)
          db.insertUserEvent(UserEvent(request.user.email, request.user.firstName, request.user.lastName, id, UserEventAccessRecipeCurationPage.name))
          // In the next instruction we reload the recipe because the recipe status could have been (and probably was) updated
          // by the previous setStatus function call. We therefore need to retrieve the latest version of the recipe.
          curatedRecipedEditor(db.getOriginalRecipe(id), editable = true, curationUser = CurationUser.getCurationUser(id, db))
        } else {
          Redirect(routes.Application.viewRecipe(recipe.id)) // redirection to read only
        }
    }
  }

  def verifyRecipe(id: String) = AuthAction { implicit request =>
    db.getOriginalRecipe(id) match {
      case None => NotFound
      case Some(recipe) =>
        // We only curate recipes that are in Ready status or PendingCuration status
        // See comment in function curateRecipe above for why we allow RecipeStatusPendingVerification and RecipeStatusPendingFinalisation in the below boolean evaluations
        if (recipe.status == RecipeStatusCurated || recipe.status == RecipeStatusPendingVerification || recipe.status == RecipeStatusVerified || recipe.status == RecipeStatusPendingFinalisation || recipe.status == RecipeStatusFinalised) {
          db.moveRecipeStatusFromStableStateToNextPendingState(id)
          db.insertUserEvent(UserEvent(request.user.email, request.user.firstName, request.user.lastName, id, UserEventAccessRecipeVerificationPage.name))
          // In the next instruction we reload the recipe because the recipe status could have been (and probably was) updated
          // by the previous setStatus function call. We therefore need to retrieve the latest version of the recipe.
          curatedRecipedEditor(db.getOriginalRecipe(id), editable = true, curationUser = CurationUser.getCurationUser(id, db))
        } else {
          Redirect(routes.Application.viewRecipe(recipe.id)) // redirection to read only
        }
    }
  }

  def verifyOneRecipe = AuthAction { implicit request =>
    val maybeRecipe = db.getOriginalRecipeInVerifiableStatus()
    maybeRecipe match {
      case Some(recipe) => Redirect(routes.Application.verifyRecipe(recipe.id))
      case None => NotFound
    }
  }

  def curateOneRecipeInReadyStatus = AuthAction { implicit request =>
    val newRecipe = db.getOriginalRecipeInReadyStatus()
    newRecipe match {
      case Some(r) => Redirect(routes.Application.curateRecipe(r.id))
      case None => NotFound
    }
  }

  private def recipeStatusToUserEventOperationType(status: RecipeStatus): String = {
    // We are currently emitting a string
    // TODO: emit a proper OperationType
    status match {
      case RecipeStatusCurated => "Curation"
      case RecipeStatusVerified => "Verification"
      case RecipeStatusFinalised => "Confirmation"
      case _ => "Curation"
    }
  }

  def postRecipeUpdate(recipeId: String) = AuthAction { implicit request =>
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
      db.moveRecipeStatusFromPendingStateToNextStableState(recipeId)
      db.getOriginalRecipeStatus(recipeId).foreach(status => db.insertUserEvent(UserEvent(request.user.email, request.user.firstName, request.user.lastName, recipeId, recipeStatusToUserEventOperationType(status))))
<<<<<<< HEAD
      RecipePublisher.publishRecipe(curatedRecipeWithId.recipeId)
=======
      RecipePublisher.publishRecipe(recipeId: String, db: DB, publisherConfig: PublisherConfig, teleporter: Teleporter, contentApi: ContentApi)
>>>>>>> 39d4fb100da8f4a39028cd184f3d8386ca80a2a5
      Redirect(routes.Application.curateOrVerify)
    })
  }

  def prepareRecipesForCuration = Action { implicit request =>
    RecipeReadiness.updateRecipesReadiness(db: DB)
    Ok(s"Operation Completed\n")
  }

  // -------------------------------------------------------

  private[this] def curatedRecipedEditor(
    recipe: Option[Recipe],
    editable: Boolean,
    curationUser: CurationUser
  )(implicit request: RequestHeader) = {
    recipe match {
      case Some(r) =>

        val curatedRecipe = db.getCuratedRecipeByRecipeId(r.id).map(CuratedRecipe.fromCuratedRecipeDB) getOrElse CuratedRecipe.fromRecipe(r)
        val curatedRecipeForm = CuratedRecipeForm.toForm(curatedRecipe)
        val images = db.getImages(r.articleId)

        logger.info(s"View ${r.id}, ${r.title}")
        Ok(views.html.recipe(
          Application.curatedRecipeForm.fill(curatedRecipeForm),
          r.id,
          r.body,
          r.articleId,
          editable,
          images,
          r.status,
          curationUser
        ))
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
        "portionType" -> text.transform[PortionType](PortionType.fromString, _.toString),
        "quantity" -> mapping(
          "from" -> of[Double],
          "to" -> of[Double]
        )(Serves.apply)(Serves.unapply),
        "unit" -> optional(text)
      )(DetailedServes.apply)(DetailedServes.unapply)),
      "ingredientsLists" -> seq(mapping(
        "title" -> optional(nonEmptyText(maxLength = 200)),
        "ingredients" -> seq(mapping(
          "quantity" -> optional(of[Double]),
          "quantityRangeFrom" -> optional(of[Double]),
          "quantityRangeTo" -> optional(of[Double]),
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
        "mediaId" -> text,
        "assetUrl" -> text,
        "altText" -> text
      )(Image.apply)(Image.unapply))
    )(CuratedRecipeForm.apply)(CuratedRecipeForm.unapply)
  )
}

