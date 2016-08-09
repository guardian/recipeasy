package etl

import com.gu.contentapi.client._
import com.gu.contentapi.client.model._
import com.gu.contentapi.client.model.v1._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}

import cats.kernel.Monoid
import cats.Foldable
import cats.std.list._
import cats.syntax.foldable._

import scala.language.higherKinds

case class Progress(pagesProcessed: Int, articlesProcessed: Int, recipesFound: Int, articlesWithNoRecipes: List[String]) {
  override def toString: String = s"$pagesProcessed pages processed,\t$articlesProcessed articles processed,\t$recipesFound recipes found,\t${articlesWithNoRecipes.size} articles with no recipes" 
}

object Progress {
  val empty = Progress(0, 0, 0, Nil)
  implicit val progressMonoid: Monoid[Progress] = new Monoid[Progress] {
    def empty = Progress.empty
    def combine(x: Progress, y: Progress) = Progress(
      x.pagesProcessed + y.pagesProcessed, 
      x.articlesProcessed + y.articlesProcessed, 
      x.recipesFound + y.recipesFound,
      x.articlesWithNoRecipes ++ y.articlesWithNoRecipes
    )
  }
}

object ETL extends App {

  if (args.isEmpty) {
    Console.err.println("Usage: ETL <CAPI key>")
    sys.exit(1)
  }

  val capiKey = args(0)
  val capiClient = new GuardianContentClient(capiKey, useThrift = true)
  val query = SearchQuery()
    .pageSize(100)
    .contentType("article") // there are some video recipes, don't want those
    .tag("tone/recipes,-lifeandstyle/series/the-lunch-box,-lifeandstyle/series/last-bites")
    .showFields("main,body")
  try {
    val firstPage = Await.result(capiClient.getResponse(query), 5.seconds)
    val pages = (1 to firstPage.pages).toList
    
    val endResult = processAllRecipeArticles(pages)
    println(s"Finished! End result: $endResult")
    println("Articles with no recipes:")
    endResult.articlesWithNoRecipes.foreach(id => println(s"- https://www.theguardian.com/$id"))
  } finally {
    capiClient.shutdown()
  }

  def processAllRecipeArticles(pages: List[Int]): Progress = {
    foldMapWithLogging(pages) { p =>
      val progress = Try(Await.result(capiClient.getResponse(query.page(p)), 5.seconds)) match {
        case Success(response) =>
          println(s"Processing page $p of CAPI results")
          processPage(response.results.toList)
        case Failure(e) =>
          println(s"Skipping page $p because of CAPI failure (${e.getMessage})")
          Progress.empty
      }

      Thread.sleep(500) // avoid spamming CAPI
      progress
    }
  }

  def processPage(contents: List[Content]): Progress = {
    val progress = contents.foldMap { content =>
      //println(s"Processing content ${content.id}")
      val recipes = RecipeExtraction.findRecipes(content.webTitle, content.fields.flatMap(_.body).getOrElse(""))
      //println(s"Found ${recipes.size} recipes:")
      //recipes.foreach(r => println(s" - ${r.title}"))
      Progress(
        pagesProcessed = 0, 
        articlesProcessed = 1, 
        recipesFound = recipes.size, 
        articlesWithNoRecipes = if (recipes.isEmpty) List(content.id) else Nil
      )
    }
    progress.copy(pagesProcessed = 1)
  }

  def foldMapWithLogging[A, B, F[_]](fa: F[A])(f: A => B)(implicit Fo: Foldable[F], B: Monoid[B]): B = {
    Fo.foldLeft(fa, B.empty){ (b, a) => 
      val progress = B.combine(b, f(a))
      println(s"Progress: $progress")
      progress
    }
  }

}
