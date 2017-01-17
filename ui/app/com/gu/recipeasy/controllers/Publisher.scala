package controllers

import java.time.OffsetDateTime

import com.amazonaws.services.kinesis.model.PutRecordResult
import com.gu.auxiliaryatom.model.auxiliaryatomevent.v1.{ AuxiliaryAtom, AuxiliaryAtomEvent, EventType => AuxiliaryAtomEventType }
import com.gu.contentatom.thrift._
import com.gu.recipeasy.auth.AuthActions
import com.gu.recipeasy.db.DB
import com.gu.recipeasy.models.{ CuratedRecipe, Recipe }
import com.gu.recipeasy.services.ContentApi
import com.typesafe.scalalogging.StrictLogging
import play.api.Configuration
import play.api.libs.ws.WSClient
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
    }
  }

}
