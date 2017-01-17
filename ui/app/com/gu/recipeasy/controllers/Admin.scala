package controllers

import java.time.OffsetDateTime

import auth.GoogleGroupsAuthorisation
import com.gu.googleauth.UserIdentity
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
import org.slf4j.LoggerFactory
import play.api.mvc.Security.AuthenticatedRequest

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import auth.Authorisation

class Admin(override val wsClient: WSClient, override val conf: Configuration, db: DB, val messagesApi: MessagesApi, googleGroupAuthorizer: Authorisation) extends Controller with AuthActions with I18nSupport with StrictLogging {

  type AuthReq[A] = AuthenticatedRequest[A, UserIdentity]

  object AdminAuditAction extends ActionFunction[AuthReq, AuthReq] {
    def invokeBlock[A](request: AuthReq[A], block: AuthReq[A] => Future[Result]): Future[Result] = {
      googleGroupAuthorizer.isAdmin(request.user.email).flatMap { isAdmin =>
        if (isAdmin) {
          block(request)
        } else {
          Future.successful(NotFound)
        }
      }
    }
  }

  private def AdminAuth: ActionBuilder[AuthReq] = AuthAction andThen AdminAuditAction

  def adminLandingPage = AdminAuth { implicit request =>
    Ok(views.html.admin.index())
  }

  def recentActivity = AdminAuth { implicit request =>
    val userEventDBs: List[UserEventDB] = db.userEvents(200)
    Ok(views.html.admin.recentactivity(userEventDBs))
  }

  def recentActivityCSV = AdminAuth { implicit request =>
    val userEventDBs: List[UserEventDB] = db.userEventsAll()
    Ok(views.html.admin.recentactivitycsv(userEventDBs)).as("text/plain")
  }

  def recentUserActivity(email: String) = AdminAuth { implicit request =>
    val userEventDBs = db.singleUserEvents(email)
    Ok(views.html.admin.recentuseractivity(email, userEventDBs))
  }

  def usersListing = AdminAuth { implicit request =>
    val usersListing: List[String] = db.userEmails()
    Ok(views.html.admin.users(usersListing))
  }

  def dailyBreakdown = AdminAuth { implicit request =>
    Ok(views.html.admin.dailybreakdown(db.dailyActivityDistribution()))
  }

  def leaderboard = AdminAuth { implicit request =>
    val leaderboard = Leaderboard.eventsToOrderedLeaderboardEntries(db.userEventsAll())
    val userspeeds = UsersSpeedsMeasurements.generalUserSpeedMapping(db)
    Ok(views.html.admin.leaderboard(leaderboard, userspeeds))
  }

  def statusDistribution = AdminAuth { implicit request =>
    val distribution: Map[RecipeStatus, Long] = Map(
      RecipeStatusNew -> db.countRecipesInGivenStatus(RecipeStatusNew),
      RecipeStatusReady -> db.countRecipesInGivenStatus(RecipeStatusReady),
      RecipeStatusPendingCuration -> db.countRecipesInGivenStatus(RecipeStatusPendingCuration),
      RecipeStatusCurated -> db.countRecipesInGivenStatus(RecipeStatusCurated),
      RecipeStatusPendingVerification -> db.countRecipesInGivenStatus(RecipeStatusPendingVerification),
      RecipeStatusVerified -> db.countRecipesInGivenStatus(RecipeStatusVerified),
      RecipeStatusPendingFinalisation -> db.countRecipesInGivenStatus(RecipeStatusPendingFinalisation),
      RecipeStatusFinalised -> db.countRecipesInGivenStatus(RecipeStatusFinalised),
      RecipeStatusImpossible -> db.countRecipesInGivenStatus(RecipeStatusImpossible)
    )
    Ok(views.html.admin.statusdistribution(distribution))
  }

}
