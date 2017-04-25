package com.gu.recipeasy.services

import com.gu.contentapi.client.GuardianContentClient
import com.gu.contentapi.client.model.v1.{ Asset, Element }
import com.gu.contentatom.thrift.{ Image, ImageAsset, ImageAssetDimensions }
import com.gu.recipeasy.models.CuratedRecipe

import scala.concurrent.{ ExecutionContext, Future }

class ContentApi(contentApiClient: GuardianContentClient) {

  def findImagesForRecipe(articleId: String, curatedRecipe: CuratedRecipe)(implicit ec: ExecutionContext): Future[Seq[Image]] = {

    val itemQuery = contentApiClient.item(articleId).showElements("image")

    def toImageAsset(asset: Asset): ImageAsset = {
      ImageAsset(
        mimeType = asset.mimeType,
        file = asset.file.getOrElse(""),
        dimensions = asset.typeData.flatMap { td => for (height <- td.height; width <- td.width) yield ImageAssetDimensions(height, width) },
        size = None
      )
    }

    def toImages(imageElements: Seq[Element]): Seq[Image] = {
      imageElements.map { imageElement =>
        val imageAssets = imageElement.assets map toImageAsset
        Image(
          assets = imageAssets.filter(!_.file.contains("/master")),
          master = imageAssets.filter(_.file.contains("/master")).headOption,
          mediaId = imageElement.id
        )
      }
    }

    contentApiClient.getResponse(itemQuery).map { recipeArticle =>

      val images = (for (content <- recipeArticle.content; elements <- content.elements) yield toImages(elements)).getOrElse(Nil)
      val imageIdsSelectedForRecipe = curatedRecipe.images.images.map(_.mediaId)

      images.filter(image => imageIdsSelectedForRecipe.contains(image.mediaId))
    }
  }

}
