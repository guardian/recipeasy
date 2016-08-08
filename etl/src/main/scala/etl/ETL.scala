package etl

import com.gu.contentapi.client._
import com.gu.contentapi.client.model._
import com.gu.contentapi.client.model.v1._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}

object ETL extends App {

  if (args.isEmpty) {
    Console.err.println("Usage: ETL <CAPI key>")
    sys.exit(1)
  }

  val capiKey = args(0)
  val capiClient = new GuardianContentClient(capiKey, useThrift = true)
  val query = SearchQuery()
    .pageSize(100)
    .tag("tone/recipes,-lifeandstyle/series/the-lunch-box,-lifeandstyle/series/last-bites")
    .showFields("main,body")
  val firstPage = Await.result(capiClient.getResponse(query), 5.seconds)
  val pages = firstPage.pages

  for (p <- 1 to pages) {
    Try(Await.result(capiClient.getResponse(query.page(p)), 5.seconds)) match {
      case Success(response) =>
        println(s"Processing page $p of CAPI results")
        processPage(response.results)
        println()
      case Failure(e) =>
        println(s"Skipping page $p because of CAPI failure (${e.getMessage})")
    }
    Thread.sleep(500) // avoid spamming CAPI
  }

  def processPage(contents: Seq[Content]): Unit = {
    contents.foreach { content =>
      println(s"Processing content ${content.id}")
      val recipes = RecipeExtraction.findRecipes(content.webTitle, content.fields.flatMap(_.body).getOrElse(""))
      println(s"Found ${recipes.size} recipes:")
      recipes.foreach(r => println(s" - ${r.title}"))
    }
  }


}
