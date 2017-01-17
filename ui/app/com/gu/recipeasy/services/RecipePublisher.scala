package services

import java.time.OffsetDateTime

import com.amazonaws.services.kinesis.model.PutRecordResult
import com.gu.auxiliaryatom.model.auxiliaryatomevent.v1.{AuxiliaryAtom, AuxiliaryAtomEvent, EventType => AuxiliaryAtomEventType}
import com.gu.contentatom.thrift._
import com.gu.recipeasy.models.{CuratedRecipe, Recipe}
import com.gu.recipeasy.services.ContentApi
import scala.concurrent.Future

import scala.util.Try

object RecipePublisher {

  def publishRecipe(recipe: Recipe, curatedRecipe: CuratedRecipe, config: PublisherConfig, teleporter: Teleporter, contentApi: ContentApi): Future[Try[List[PutRecordResult]]] = {
    val futureImages = contentApi.findImagesForRecipe(recipe.articleId, curatedRecipe)
    val futureInternalComposerCode = teleporter.getInternalComposerCode(recipe.articleId)
    for {
      curatedRecipeImages <- futureImages
      internalComposerCode <- futureInternalComposerCode
    } yield {
      send(internalComposerCode, recipe, curatedRecipe, curatedRecipeImages)(config)
    }
  }

  private def send(internalComposerCode: String, recipe: Recipe, curatedRecipe: CuratedRecipe, curatedRecipeImages: Seq[Image])(config: PublisherConfig): Try[List[PutRecordResult]] = {
    val atomEvents: (AuxiliaryAtomEvent, ContentAtomEvent) = {
      val contentAtom = CuratedRecipe.toAtom(recipe, curatedRecipe, curatedRecipeImages)
      val auxiliaryAtomEvent = AuxiliaryAtomEvent(internalComposerCode, eventType = AuxiliaryAtomEventType.Add, Seq(AuxiliaryAtom(contentAtom.id, "recipe")))
      val contentAtomEvent = ContentAtomEvent(contentAtom, EventType.Update, eventCreationTime = OffsetDateTime.now.toInstant.toEpochMilli)
      (auxiliaryAtomEvent, contentAtomEvent)
    }
    AtomPublisher.send(atomEvents)(config)
  }

}