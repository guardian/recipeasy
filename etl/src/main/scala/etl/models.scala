package etl

import org.jsoup.nodes._

case class RawRecipe(
  title: String, 
  body: Seq[Element]
)

case class ParsedRecipe(
  raw: RawRecipe,
  guesses: Guesses
)

case class Guesses(
  serves: Option[Int] = None,
  ingredients: Option[Seq[String]] = None,
  image: Option[String] = None
)

