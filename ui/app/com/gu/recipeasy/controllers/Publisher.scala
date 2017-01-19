package controllers

import com.amazonaws.services.kinesis.model.PutRecordResult
import com.gu.recipeasy.auth.AuthActions
import com.gu.recipeasy.db.DB
import com.gu.recipeasy.services.{ ContentApi, PublisherConfig, RecipePublisher, Teleporter }
import com.typesafe.scalalogging.StrictLogging
import play.api.Configuration
import play.api.libs.ws.WSClient
import play.api.mvc.{ Action, Controller }

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
