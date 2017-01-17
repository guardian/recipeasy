package controllers

import java.time.OffsetDateTime

import com.amazonaws.services.kinesis.model.PutRecordResult
<<<<<<< HEAD
import com.gu.auxiliaryatom.model.auxiliaryatomevent.v1.{AuxiliaryAtom, AuxiliaryAtomEvent, EventType => AuxiliaryAtomEventType}
import com.gu.contentatom.thrift._
import com.gu.recipeasy.auth.AuthActions
import com.gu.recipeasy.db.DB
import com.gu.recipeasy.models.{CuratedRecipe, Recipe}
=======
import com.gu.auxiliaryatom.model.auxiliaryatomevent.v1.{ AuxiliaryAtom, AuxiliaryAtomEvent, EventType => AuxiliaryAtomEventType }
import com.gu.contentatom.thrift._
import com.gu.recipeasy.auth.AuthActions
import com.gu.recipeasy.db.DB
import com.gu.recipeasy.models.{ CuratedRecipe, Recipe }
>>>>>>> 39d4fb100da8f4a39028cd184f3d8386ca80a2a5
import com.gu.recipeasy.services.ContentApi
import com.typesafe.scalalogging.StrictLogging
import play.api.Configuration
import play.api.libs.ws.WSClient
<<<<<<< HEAD
import play.api.mvc.{Action, Controller}
import services.{AtomPublisher, PublisherConfig, Teleporter}
import services.RecipePublisher

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class Publisher(override val wsClient: WSClient, override val conf: Configuration, config: PublisherConfig, db: DB, teleporter: Teleporter, contentApi: ContentApi) extends Controller with AuthActions with StrictLogging {

  /*
val result = RecipePublisher.publishRecipe(recipe: Recipe, curatedRecipe: CuratedRecipe, config: PublisherConfig, teleporter: Teleporter, contentApi: ContentApi)
result match {
  case Success(_) => Ok(s"Publishing content atom")
  case Failure(error) =>
    logger.warn(s"Fail to publish content atom ${error.getMessage}", error)
    Ok(s"Failed to publish content atom: ${error.getMessage}")
}

*/

  def publish(id: String) = Action.async { implicit request =>
    db.getOriginalRecipe(id) match {
      case Some(recipe) => {
        db.getCuratedRecipeByRecipeId(recipe.id).map(CuratedRecipe.fromCuratedRecipeDB) match {
          case Some(curatedRecipe) => {
            val fresult:Future[Try[List[PutRecordResult]]] = RecipePublisher.publishRecipe(recipe: Recipe, curatedRecipe: CuratedRecipe, config: PublisherConfig, teleporter: Teleporter, contentApi: ContentApi)
            fresult.map{ result =>
              result match {
                case Success(_) => Ok(s"Publishing content atom")
                case Failure(error) =>
                  logger.warn(s"Fail to publish content atom ${error.getMessage}", error)
                  Ok(s"Failed to publish content atom: ${error.getMessage}")
              }
            }
            Future.successful(Ok(s"Publishing content atom"))
          }
          case None => Future.successful(NotFound)
        }
      }
      case None => Future.successful(NotFound)
=======
import play.api.mvc.{ Action, Controller }
import services.{ AtomPublisher, PublisherConfig, Teleporter }
import services.RecipePublisher

import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }

import scala.concurrent.ExecutionContext.Implicits.global

class Publisher(override val wsClient: WSClient, override val conf: Configuration, publisherConfig: PublisherConfig, db: DB, teleporter: Teleporter, contentApi: ContentApi) extends Controller with AuthActions with StrictLogging {

  def publish(recipeId: String) = Action.async { implicit request =>
    val fresult: Future[Try[List[PutRecordResult]]] = RecipePublisher.publishRecipe(recipeId, db, publisherConfig, teleporter, contentApi)
    fresult.map { result =>
      result match {
        case Success(_) => Ok(s"Publishing content atom")
        case Failure(error) =>
          logger.warn(s"Fail to publish content atom ${error.getMessage}", error)
          Ok(s"Failed to publish content atom: ${error.getMessage}")
      }
>>>>>>> 39d4fb100da8f4a39028cd184f3d8386ca80a2a5
    }
  }

}
