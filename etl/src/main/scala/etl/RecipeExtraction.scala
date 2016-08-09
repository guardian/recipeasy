package etl

import org.jsoup.Jsoup
import org.jsoup.nodes._
import cats.data.NonEmptyList

import scala.collection.JavaConverters._

object RecipeExtraction {

  def findRecipes(articleTitle: String, articleBodyHtml: String): Seq[RawRecipe] = {
    val doc = Jsoup.parse(articleBodyHtml)
    val recipes = {
      val split = filterOutNonRecipes(splitIntoRecipes(doc))
      if (split.nonEmpty) {
        split
      } else {
        // treat the whole article as one recipe
        filterOutNonRecipes(List(RawRecipe(articleTitle, doc.body.children.asScala)))
      }
    }
    //println(recipes.map(_.title))
    recipes
  }

  private def filterOutNonRecipes(candidates: Seq[RawRecipe]): Seq[RawRecipe] = {
    val (keep, discard) = candidates.partition(looksLikeRecipe _)
    //if (discard.nonEmpty) println(s"Discarding ${discard.size} non-recipes")
    keep
  }

  private def looksLikeRecipe(candidate: RawRecipe): Boolean = {
    // A few simple heuristics to predict whether a block of HTML is a recipe
    val terms = candidate.body.map(_.text).mkString(" ").split(" ").toSet
    terms.contains("Serves") || terms.contains("Makes") ||
    terms.contains("Ingredients") ||
      terms.contains("tsp") || terms.contains("tbsp") || terms.exists(_.matches("\\d+(ml|g)"))
  }

  private def splitIntoRecipes(doc: Document): Seq[RawRecipe] = {
    val recipeTitles = findRecipeTitles(doc)
    val minusPreamble = doc.body.children.asScala.toList.dropWhile(e => !recipeTitles.contains(e))
    val chunks = groupPrefix(minusPreamble)(recipeTitles.contains)
    chunks.map { nel => 
      val title = nel.head.text
      val body = nel.tail
      RawRecipe(title, body)
    }
  }

  private def findRecipeTitles(doc: Document): Seq[Element] = {
    doc.select("h2").asScala.filterNot(recipeTitleBlacklist)
    // TODO in very old articles they used string instead of h2, could look for those as well
  }

  private def recipeTitleBlacklist(elem: Element): Boolean = {
    val text = elem.text.trim.toLowerCase
    text.isEmpty || 
      text.startsWith("for the") // e.g. "for the dressing"
  }

  private def groupPrefix[T](xs: List[T])(p: T => Boolean): List[NonEmptyList[T]] = xs match {
   case List() => List()
   case x :: xs1 => 
     val (ys, zs) = xs1 span (!p(_))
     NonEmptyList(x, ys) :: groupPrefix(zs)(p)  
 }

}
