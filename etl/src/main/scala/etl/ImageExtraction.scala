package etl

import com.gu.contentapi.client.model.v1.{ Asset, Content, Element }
import com.gu.recipeasy.models.{ ImageDB }

object ImageExtraction {

  def getImages(maybeContent: Option[Content]): Seq[ImageDB] = {
    maybeContent match {
      case Some(content) => findImageElements(content).flatMap(i => extractImageData(i, content.id))
      case _ => Nil
    }
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
      typeData <- largestAsset.typeData
      altText <- typeData.altText
      assetUrl <- typeData.secureFile
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
