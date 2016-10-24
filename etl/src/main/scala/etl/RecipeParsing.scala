package etl

import scala.util.matching.Regex
import org.jsoup.Jsoup
import org.jsoup.nodes._
import cats.data.NonEmptyList
import com.gu.recipeasy.models._

import scala.collection.JavaConverters._

object RecipeParsing {

  def parseRecipe(rawRecipe: RawRecipe): ParsedRecipe = {
    val serves = guessServes(rawRecipe.body)
    val ingredients = guessIngredients(rawRecipe.body)
    val steps = guessSteps(rawRecipe.body)

    ParsedRecipe(
      rawRecipe.title,
      rawRecipe.body.mkString("\n"),
      serves,
      ingredients,
      steps
    )
  }

  // Nobody makes food for more than 12 people, right?
  private val ServesSimple = """\(?serves (\d+|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve)\)?""".r.unanchored

  // TODO "serves 4-6"
  def guessServes(body: Seq[Element]): Option[Serves] = {
    val text = body.map(_.text.toLowerCase.trim)
    val servesCount = text.collectFirst {
      case ServesSimple(number) => number match {
        case "one" => 1
        case "two" => 2
        case "three" => 3
        case "four" => 4
        case "five" => 5
        case "six" => 6
        case "seven" => 7
        case "eight" => 8
        case "nine" => 9
        case "ten" => 10
        case "eleven" => 11
        case "twelve" => 12
        case digits => digits.toInt
      }
    }
    servesCount.map(s => Serves(from = s, to = s))
  }

  def guessIngredients(body: Seq[Element]): Seq[IngredientsList] = {
    // Find paragraphs containing short text items separated by <br>
    val candidates = body.filter(ParaWithListOfShortTexts.matches)

    candidates.flatMap(buildIngredientList)
  }

  private def buildIngredientList(para: Element): Option[IngredientsList] = {
    def text(node: Node): String = node match {
      case tn: TextNode => tn.text.trim
      case elem: Element => elem.text.trim
      case other => ""
    }
    val listItems: Seq[Node] = para.childNodes.asScala.filterNot(_.nodeName == "br")
    val withoutServingCount = listItems.filterNot(n => text(n).toLowerCase.dropWhile(_ == '(').startsWith("serves"))
    if (withoutServingCount.size < 2) None
    else {
      if (withoutServingCount(0).nodeName != withoutServingCount(1).nodeName) {
        val title = text(withoutServingCount(0))
        val ingredients = withoutServingCount.drop(1).map(text)
        Some(IngredientsList(title = Some(title), ingredients))
      } else {
        val ingredients = withoutServingCount.map(text)
        Some(IngredientsList(title = None, ingredients))
      }
    }
  }

  def guessSteps(body: Seq[Element]): Seq[String] = {
    val candidates: Seq[Element] = body.filter(NumberedParagraph.matches)
    val stepsFound = candidates.map(_.text).filter(_.nonEmpty)
    val pattern = new scala.util.matching.Regex("""(^\d+.?\s?)(.*+)""", "number", "step")

    stepsFound.map(s => pattern replaceAllIn (s, m => m.group("step")))
  }

  /**
   * Extractor to match a <p> containing a list of short pieces of text separated by <br>.
   * The list items may or may not be wrapped in <strong>.
   * e.g.
   *
   * {{{
   * <p>
   *   <strong>short piece of text</strong>
   *   <br>
   *   another piece of text
   *   <br>
   *   one more
   * </p>
   * }}}
   */
  private object ParaWithListOfShortTexts {

    def matches(el: Element): Boolean = {
      if (el == null) false
      else {
        if (el.tag.getName == "p") {
          val pairs = el.childNodes.asScala.toList.grouped(2)
          pairs.forall {
            case x :: y :: Nil => ShortListItem.matches(x) && y.nodeName == "br"
            case x :: Nil => ShortListItem.matches(x) // final element
            case _ => false
          }
        } else false
      }
    }

    def unapply(el: Element): Option[Element] =
      if (matches(el)) Some(el) else None

  }

  private object ShortListItem {

    private def isShort(length: Int) = length > 0 && length < 120

    def matches(node: Node): Boolean = node match {
      case tn: TextNode => isShort(tn.text.trim.size)
      case elem: Element if elem.tag.getName == "strong" => isShort(elem.text.trim.size)
      case other => false
    }

  }

  private object NumberedParagraph {

    def matches(el: Element): Boolean = {
      if (el == null) false
      else {
        el.tag.getName == "p" && el.text.headOption.exists(_.isDigit) && !ParaWithListOfShortTexts.matches(el)
      }
    }

    def unapply(el: Element): Option[Element] = {
      if (matches(el)) Some(el) else None
    }

  }

}
