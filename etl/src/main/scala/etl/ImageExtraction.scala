package etl

import com.gu.contentapi.client.model.v1.{ Asset, Content, Element }
import com.gu.recipeasy.models.{ Image }

object ImageExtraction {

  def getImages(content: Content, articleId: String): Seq[Image] = {
    val images = findImageElements(content)
    images match {
      case Some(images) => images.flatMap(i => extractImageData(i, articleId))
      case None => Nil
    }
  }

  private def findImageElements(content: Content): Option[Seq[Element]] = {
    val elems = for {
      elements <- content.elements
      imageElements = elements.filter(e => e.relation != "thumbnail")
    } yield imageElements
    elems
  }

  private def extractImageData(image: Element, articleId: String): Option[Image] = {
    for {
      largestAsset <- findLargestAsset(image)
      assetUrl <- largestAsset.file
      typeData <- largestAsset.typeData
      altText <- typeData.altText
    } yield new Image(
      image.id,
      articleId,
      assetUrl,
      altText
    )
  }

  private def findLargestAsset(element: Element): Option[Asset] = {
    element.assets.sortBy(_.typeData.flatMap(_.width)).lastOption
  }

}
