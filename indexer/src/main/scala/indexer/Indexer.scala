package indexer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.amazonaws.regions.{ Region, Regions }
import com.amazonaws.services.kinesis.model.PutRecordResult
import com.gu.contentapi.client.GuardianContentClient
import com.gu.recipeasy.db.{ ContextWrapper, DB }
import com.gu.recipeasy.services._
import com.typesafe.config.ConfigFactory
import play.api.Configuration
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }
import scala.concurrent.ExecutionContext.Implicits.global

object Indexer extends App {

  if (args.isEmpty) {
    Console.err.println("Usage: Indexer <STAGE>")
    sys.exit(1)
  }

  val stage = args(0)
  val contextWrapper = new ContextWrapper { val config = ConfigFactory.load() }
  val config = new Configuration(ConfigFactory.load())
  val db = new DB(contextWrapper)

  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()
  val teleporter = new Teleporter(AhcWSClient())
  val region = Region.getRegion(Regions.fromName(Regions.EU_WEST_1.getName))

  val publisherConfig = PublisherConfig(config, region, stage)

  val contentApiClient = new ContentApi(contentApiClient = new GuardianContentClient(publisherConfig.contentAtomConfig.capiKey))

  val curatedRecipeIds: List[String] = db.getCuratedRecipesIds()

  curatedRecipeIds.foreach { r =>
    val fresult: Future[Try[List[PutRecordResult]]] = RecipePublisher.publishRecipe(r, db, publisherConfig, teleporter, contentApiClient)
    fresult.map { result =>
      result match {
        case Success(_) => println(s"Successfully published $r")
        case Failure(error) => println(s"Failed to published $r")
      }
    }

    Thread.sleep(1000)
  }

}
