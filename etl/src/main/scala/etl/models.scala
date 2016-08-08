package etl

import org.jsoup.nodes._

case class RawRecipe(
  title: String, 
  body: Seq[Element], 
  ingredientsListHtmlGuess: Option[String],
  imageGuess: Option[String])
