package controllers

import java.time.OffsetDateTime

import com.amazonaws.services.kinesis.model.PutRecordResult
import com.gu.auxiliaryatom.model.auxiliaryatomevent.v1.{AuxiliaryAtom, AuxiliaryAtomEvent, EventType => AuxiliaryAtomEventType}
import com.gu.contentatom.thrift._
import com.gu.recipeasy.auth.AuthActions
import com.gu.recipeasy.db.DB
import com.gu.recipeasy.models.{CuratedRecipe, Recipe}
import com.gu.recipeasy.services.ContentApi
import com.typesafe.scalalogging.StrictLogging
import play.api.Configuration
import play.api.libs.ws.WSClient
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
    }
  }

  private def send(internalComposerCode: String, recipe: Recipe, curatedRecipe: CuratedRecipe, curatedRecipeImages: Seq[Image])(config: PublisherConfig) = {

    val atomEvents: (AuxiliaryAtomEvent, ContentAtomEvent) = {
      val contentAtom = CuratedRecipe.toAtom(recipe, curatedRecipe, curatedRecipeImages)
      val auxiliaryAtomEvent = AuxiliaryAtomEvent(internalComposerCode, eventType = AuxiliaryAtomEventType.Add, Seq(AuxiliaryAtom(contentAtom.id, "recipe")))
      val contentAtomEvent = ContentAtomEvent(contentAtom, EventType.Update, eventCreationTime = OffsetDateTime.now.toInstant.toEpochMilli)
      (auxiliaryAtomEvent, contentAtomEvent)
    }

    AtomPublisher.send(atomEvents)(config)
  }

}
