package services

import java.util.concurrent.Executors
import play.api.libs.ws.WSClient
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Await }
import scala.util.Try

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