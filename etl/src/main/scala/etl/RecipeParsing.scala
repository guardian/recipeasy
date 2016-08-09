package etl

import org.jsoup.Jsoup
import org.jsoup.nodes._
import cats.data.NonEmptyList

import scala.collection.JavaConverters._

object RecipeParsing {

  def parseRecipe(rawRecipe: RawRecipe): ParsedRecipe = {
    val serves = guessServes(rawRecipe.body)
    val ingredients = guessIngredients(rawRecipe.body)

    // Do image-finding later, as we need to look through the whole article
    ParsedRecipe(rawRecipe, Guesses(serves, ingredients))
  }

  // Nobody makes food for more than 12 people, right?
  private val Serves = """\(?serves (\d+|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve)\)?""".r

  def guessServes(body: Seq[Element]): Option[Int] = {
    body.map(_.text.toLowerCase.trim).collectFirst {
      case Serves(number) => number match {
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
  }

  def guessIngredients(body: Seq[Element]) = {
    // TODO
    None
  }

}
