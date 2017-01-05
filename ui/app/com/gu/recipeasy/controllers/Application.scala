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
      case None => {
        db.getOriginalRecipeInReadyStatus match {
          case Some(recipe) => Redirect(routes.Application.curateRecipe(recipe.id))
          case None => NotFound
        }
      }
    }
  }

  def curateRecipe(id: String) = AuthAction { implicit request =>
    val maybeRecipe = db.getOriginalRecipe(id)
    maybeRecipe match {
      case None => NotFound
      case Some(recipe) => {
        if (recipe.status == RecipeStatusReady || recipe.status == RecipeStatusPending) {
          db.setOriginalRecipeStatus(recipe.id, RecipeStatusPending)
          db.insertUserEvent(UserEvent(request.user.email, request.user.firstName, request.user.lastName, id, UserEventAccessRecipeCurationPage.name))
          curatedRecipedEditor(db.getOriginalRecipe(id), editable = true, curationUser = CurationUser.getCurationUser(id, db))
        } else {
          Redirect(routes.Application.viewRecipe(recipe.id)) // redirection to read only
        }
      }
    }

  }

  def verifyRecipe(id: String) = AuthAction { implicit request =>
    val recipe = db.getOriginalRecipe(id)
    // We reuse the code for `curateRecipe` because curation and verification use the same logic and the same editor
    // But we need to record the fact that the recipe is being verified.
    db.insertUserEvent(UserEvent(request.user.email, request.user.firstName, request.user.lastName, id, UserEventAccessRecipeVerificationPage.name))
    curatedRecipedEditor(recipe, editable = true, curationUser = CurationUser.getCurationUser(id, db))
  }

  def finalCheckRecipe(id: String) = AuthAction { implicit request =>
    val recipe = db.getOriginalRecipe(id)
    // We reuse the code for `curateRecipe` because curation and verification use the same logic and the same editor
    // But we need to record the fact that the recipe is being verified.
    curatedRecipedEditor(recipe, editable = true, curationUser = CurationUser.getCurationUser(id, db))
  }

  def curateOneRecipeInNewStatus = AuthAction { implicit request =>
    val newRecipe = db.getOriginalRecipeInNewStatus
    newRecipe match {
      case Some(r) => Redirect(routes.Application.curateRecipe(r.id))
      case None => NotFound
    }
  }

  def verifyOneRecipe = AuthAction { implicit request =>
    val maybeRecipe = db.getOriginalRecipeInVerifiableStatus
    maybeRecipe match {
      case Some(recipe) => Redirect(routes.Application.verifyRecipe(recipe.id))
      case None => NotFound
    }
  }

  def curateOneRecipeInReadyStatus = AuthAction { implicit request =>
    val newRecipe = db.getOriginalRecipeInReadyStatus
    newRecipe match {
      case Some(r) => Redirect(routes.Application.curateRecipe(r.id))
      case None => NotFound
    }
  }

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
      db.getOriginalRecipeStatus(recipeId).foreach(status => db.insertUserEvent(UserEvent(request.user.email, request.user.firstName, request.user.lastName, recipeId, recipeStatusToUserEventOperationType(status))))
      Redirect(routes.Application.curateOneRecipeInReadyStatus)
    })
  }

  def adminLandingPage = AuthAction { implicit request =>
    Ok(views.html.admin.index())
  }

  def recentActivity = AuthAction { implicit request =>
    val userEventDBs: List[UserEventDB] = db.userEvents(200)
    Ok(views.html.admin.recentactivity(userEventDBs))
  }

  def recentActivityCSV = AuthAction { implicit request =>
    val userEventDBs: List[UserEventDB] = db.userEventsAll()
    Ok(views.html.admin.recentactivitycsv(userEventDBs)).as("text/plain")
  }

  def usersListing = AuthAction { implicit request =>
    val usersListing: List[String] = db.userEmails()
    Ok(views.html.admin.users(usersListing))
  }

  def dailyBreakdown = AuthAction { implicit request =>
    Ok(views.html.admin.dailybreakdown(db.dailyActivityDistribution()))
  }

  def leaderboard = AuthAction { implicit request =>

    def userIsWhiteListed(email: String): Boolean = {
      val allowedUsers = List(
        "alastair.jardine@guardian.co.uk",
        "nathan.good@guardian.co.uk",
        "pascal.honore@guardian.co.uk"
      )
      allowedUsers.contains(email)
    }

    if (userIsWhiteListed(request.user.email)) {
      val leaderboard = Leaderboard.eventsToOrderedLeaderboardEntries(db.userEventsAll())
      Ok(views.html.admin.leaderboard(leaderboard))
    } else {
      Redirect(routes.Application.adminLandingPage)
    }

  }

  def statusDistribution = AuthAction { implicit request =>
    val distribution: Map[RecipeStatus, Long] = Map(
      RecipeStatusNew -> db.countRecipesInGivenStatus(RecipeStatusNew),
      RecipeStatusReady -> db.countRecipesInGivenStatus(RecipeStatusReady),
      RecipeStatusPending -> db.countRecipesInGivenStatus(RecipeStatusPending),
      RecipeStatusCurated -> db.countRecipesInGivenStatus(RecipeStatusCurated),
      RecipeStatusVerified -> db.countRecipesInGivenStatus(RecipeStatusVerified),
      RecipeStatusFinalised -> db.countRecipesInGivenStatus(RecipeStatusFinalised),
      RecipeStatusImpossible -> db.countRecipesInGivenStatus(RecipeStatusImpossible)
    )
    Ok(views.html.admin.statusdistribution(distribution))
  }

  def prepareRecipesForCuration = Action { implicit request =>
    RecipeReadiness.updateRecipesReadiness(db: DB)
    Ok(s"Operation Completed\n")
  }

  // -------------------------------------------------------

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

  private[this] def curatedRecipedEditor(
    recipe: Option[Recipe],
    editable: Boolean,
    curationUser: CurationUser
  )(implicit request: RequestHeader) = {
    recipe match {
      case Some(r) => {

        /* if recipe has not being edited yet, mark as currently edited */
        if (r.status == RecipeStatusNew && editable) {
          db.setOriginalRecipeStatus(r.id, RecipeStatusPending)
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
          editable,
          images,
          r.status,
          curationUser
        ))

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
        "mediaId" -> (text),
        "assetUrl" -> (text),
        "altText" -> (text)
      )(Image.apply)(Image.unapply))
    )(CuratedRecipeForm.apply)(CuratedRecipeForm.unapply)
  )
}

