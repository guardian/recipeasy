package controllers

import com.gu.recipeasy.auth.AuthActions
import com.gu.recipeasy.db.DB
import com.gu.recipeasy.models.{ Recipe, CuratedRecipe }
import com.gu.contentatom.thrift.{ ContentAtomEvent, EventType }
import com.gu.auxiliaryatom.model.auxiliaryatomevent.v1.{ AuxiliaryAtom, AuxiliaryAtomEvent, EventType => AuxiliaryAtomEventType }

import java.time.OffsetDateTime

import services.{ AtomPublisher, PublisherConfig, Teleporter }

import play.api.Configuration
import play.api.libs.ws.WSClient
import play.api.mvc.{ Action, Controller }

import scala.util.{ Failure, Success, Try }
import scala.concurrent.Future

class Publisher(override val wsClient: WSClient, override val conf: Configuration, config: PublisherConfig, db: DB, teleporter: Teleporter) extends Controller with AuthActions {

  def publish(id: String) = AuthAction.async { implicit request =>
    db.getOriginalRecipe(id) match {

      case Some(recipe) =>
        db.getCuratedRecipeByRecipeId(recipe.id).map(CuratedRecipe.fromCuratedRecipeDB) match {
          case Some(curatedRecipe) =>

            import play.api.libs.concurrent.Execution.Implicits.defaultContext

            teleporter.getInternalComposerCode(recipe.articleId).map { internalComposerCode =>
              val result = send(internalComposerCode, recipe, curatedRecipe)(config)
              result match {
                case Success(_) => Ok(s"Publishing content atom")
                case Failure(error) => Ok(s"Failed to publish content atom: ${error.getMessage}")
              }
            }

          case None => Future.successful(NotFound)
        }
      case None => Future.successful(NotFound)
    }
  }

  private def send(internalComposerCode: String, recipe: Recipe, curatedRecipe: CuratedRecipe)(config: PublisherConfig) = {

    val atomEvents: (AuxiliaryAtomEvent, ContentAtomEvent) = {
      val contentAtom = CuratedRecipe.toAtom(recipe, curatedRecipe)
      val auxiliaryAtomEvent = AuxiliaryAtomEvent(internalComposerCode, eventType = AuxiliaryAtomEventType.Add, Seq(AuxiliaryAtom(contentAtom.id, "recipe")))
      val contentAtomEvent = ContentAtomEvent(contentAtom, EventType.Update, eventCreationTime = OffsetDateTime.now.toInstant.toEpochMilli)
      (auxiliaryAtomEvent, contentAtomEvent)
    }

    AtomPublisher.send(atomEvents)(config)
  }

}
