package etl

import com.gu.contentapi.client.model.v1.{ Asset, Content, Element }
import com.gu.recipeasy.models.{ ImageDB }

object ImageExtraction {

  def getImages(content: Content, articleId: String): Seq[ImageDB] = {
    findImageElements(content).flatMap(i => extractImageData(i, articleId))
  }

  private def findImageElements(content: Content): Seq[Element] = {
    (for {
      elements <- content.elements
      imageElements = elements.filter(e => e.relation != "thumbnail")
    } yield imageElements).getOrElse(Nil)
  }

  private def extractImageData(image: Element, articleId: String): Option[ImageDB] = {
    for {
      largestAsset <- findLargestAsset(image)
      assetUrl <- largestAsset.file
      typeData <- largestAsset.typeData
      altText <- typeData.altText
    } yield new ImageDB(
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
