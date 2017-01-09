package controllers

import com.gu.recipeasy.auth.AuthActions
import com.gu.recipeasy.db.DB
import com.gu.recipeasy.models.{ CuratedRecipe, Images, Recipe }
import com.gu.contentatom.thrift._
import com.gu.auxiliaryatom.model.auxiliaryatomevent.v1.{ AuxiliaryAtom, AuxiliaryAtomEvent, EventType => AuxiliaryAtomEventType }
import java.time.OffsetDateTime

import com.gu.recipeasy.services.ContentApi
import services.{ AtomPublisher, PublisherConfig, Teleporter }
import play.api.Configuration
import play.api.libs.ws.WSClient
import play.api.mvc.Controller

import scala.util.{ Failure, Success }
import scala.concurrent.Future
import com.typesafe.scalalogging.StrictLogging

class Publisher(override val wsClient: WSClient, override val conf: Configuration, config: PublisherConfig, db: DB, teleporter: Teleporter, contentApi: ContentApi) extends Controller with AuthActions with StrictLogging {

  def publish(id: String) = AuthAction.async { implicit request =>
    db.getOriginalRecipe(id) match {

      case Some(recipe) =>
        db.getCuratedRecipeByRecipeId(recipe.id).map(CuratedRecipe.fromCuratedRecipeDB) match {
          case Some(curatedRecipe) =>

            import play.api.libs.concurrent.Execution.Implicits.defaultContext

            val futureImages = contentApi.findImagesForRecipe(recipe.articleId, curatedRecipe)
            val futureInternalComposerCode = teleporter.getInternalComposerCode(recipe.articleId)

            for {
              curatedRecipeImages <- futureImages
              internalComposerCode <- futureInternalComposerCode
            } yield {
              val result = send(internalComposerCode, recipe, curatedRecipe, curatedRecipeImages)(config)
              result match {
                case Success(_) => Ok(s"Publishing content atom")
                case Failure(error) =>
                  logger.warn(s"Fail to publish content atom ${error.getMessage}", error)
                  Ok(s"Failed to publish content atom: ${error.getMessage}")
              }
            }

          case None => Future.successful(NotFound)
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
