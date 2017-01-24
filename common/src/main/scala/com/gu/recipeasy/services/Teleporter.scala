package com.gu.recipeasy.services

import play.api.libs.ws.WSClient
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class Teleporter(ws: WSClient) {

  def getInternalComposerCode(path: String)(implicit ec: ExecutionContext): Future[String] = {

    val teleporterURI = s"https://teleporter.gutools.co.uk/api/pages?uri=https://www.theguardian.com/$path"
    ws.url(teleporterURI).get map { response =>
      (response.json \ "identifiers" \ "composerId").as[String]
    }
  }

}