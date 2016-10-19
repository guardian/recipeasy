package etl

import com.gu.contentapi.client._
import com.gu.contentapi.client.model._
import com.gu.contentapi.client.model.v1._
import com.gu.recipeasy.models._
import com.gu.recipeasy.db._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }
import cats.kernel.Monoid
import cats.Foldable
import cats.std.list._
import cats.syntax.foldable._

import scala.language.higherKinds
import java.time.OffsetDateTime
import org.apache.commons.codec.digest.DigestUtils._

import io.getquill._

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
  val query = SearchQuery()
    .pageSize(100)
    .contentType("article") // there are some video recipes, don't want those
    .tag("tone/recipes,-lifeandstyle/series/the-lunch-box,-lifeandstyle/series/last-bites,-lifeandstyle/series/breakfast-of-champions")
    .showFields("main,body,byline")
    .showElements("image")

  val dbContext = new JdbcContext[PostgresDialect, SnakeCase]("db.ctx")
  try {
    implicit val db = new DB(dbContext)
    implicit val capiClient = new GuardianContentClient(capiKey)
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
  } finally {
    dbContext.close()
  }

  def processAllRecipeArticles(pages: List[Int])(implicit capiClient: GuardianContentClient, db: DB): Progress = {
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

  def processPage(contents: List[Content])(implicit db: DB): Progress = {

    processPageWhenInserting(contents)
    processPageWhenUpdating(contents)
  }

  def processPageWhenInserting(contents: List[Content])(implicit db: DB): Progress = {
    val existingArticlesIds = db.existingArticlesIds()
    val freshContent = contents.filterNot(c => existingArticlesIds.contains(c.id))

    val progress = freshContent.foldMap { content =>
      println(s"Processing fresh content ${content.id}")
      val recipes = getRecipes(content)(db.insertImages(_))
      //println(s"Found ${recipes.size} recipes:")
      recipes.foreach(r => println(s" - ${r.title}, \n  -${r.serves}, \n   -${r.ingredientsLists}, \n    -${r.steps}"))
      println()
      db.insertAll(recipes.toList)
      makeProgress(recipes, content)
    }
    progress.copy(pagesProcessed = 1)
  }

  def processPageWhenUpdating(contents: List[Content])(implicit db: DB): Progress = {
    val existingArticlesIds = db.existingArticlesIds()
    val articlesWithEditedRecipesId = db.editedArticlesIds()
    //if a user has edited a recipe we do not want to reparse its parent article
    val articlesSafeToProcess = existingArticlesIds.diff(articlesWithEditedRecipesId)
    val articlesToUpdate = contents.filter(c => articlesSafeToProcess.contains(c.id))

    val progress = articlesToUpdate.foldMap { content =>
      println(s"Processing content ${content.id}")
      val noOp: List[ImageDB] => Unit = (_) => Unit
      val recipes = getRecipes(content)(noOp)
      //println(s"Found ${recipes.size} recipes:")
      recipes.foreach(r => println(s" - ${r.title}, \n  -${r.serves}, \n   -${r.ingredientsLists}, \n    -${r.steps}"))
      println()
      db.updateAll(recipes.toList)
      makeProgress(recipes, content)

    }
    progress.copy(pagesProcessed = 1)
  }

  def getRecipes(content: Content)(insertImages: List[ImageDB] => Unit): Seq[Recipe] = {

    val rawRecipes = RecipeExtraction.findRecipes(content.webTitle, content.fields.flatMap(_.body).getOrElse(""))
    if (rawRecipes.nonEmpty) {
      val images = ImageExtraction.getImages(content, content.id).toList
      insertImages(images)
    }
    val parsedRecipes = rawRecipes.map(RecipeParsing.parseRecipe)
    val publicationDate = content.webPublicationDate.map(time => OffsetDateTime.parse(time.iso8601)).getOrElse(OffsetDateTime.now)

    parsedRecipes.zipWithIndex.map {
      case (r, i) =>
        // include index because some recipes inside the same article have duplicate titles! (due to a parser bug)
        val id = md5Hex(s"${r.title} :: ${content.id} :: $i")
        Recipe(
          id = id,
          title = r.title,
          body = r.body,
          serves = r.serves,
          ingredientsLists = IngredientsLists(r.ingredientsLists),
          articleId = content.id,
          credit = content.fields.flatMap(_.byline),
          publicationDate,
          status = New,
          steps = Steps(r.steps)
        )
    }
  }

  def makeProgress(recipes: Seq[Recipe], content: Content): Progress = {
    Progress(
      pagesProcessed = 0,
      articlesProcessed = 1,
      recipesFound = recipes.size,
      articlesWithNoRecipes = if (recipes.isEmpty) List(content.id) else Nil
    )
  }

  def foldMapWithLogging[A, B, F[_]](fa: F[A])(f: A => B)(implicit Fo: Foldable[F], B: Monoid[B]): B = {
    Fo.foldLeft(fa, B.empty) { (b, a) =>
      val progress = B.combine(b, f(a))
      println(s"Progress: $progress")
      progress
    }
  }

}

