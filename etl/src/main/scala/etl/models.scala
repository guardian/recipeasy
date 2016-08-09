package etl

import org.jsoup.nodes._

case class RawRecipe(
  title: String, 
  body: Seq[Element]
)

case class ParsedRecipe(
  raw: RawRecipe,
  serves: Option[Int] = None,
  ingredientsLists: Seq[IngredientsList] = Nil,
  image: Option[String] = None
)

case class IngredientsList(
  title: Option[String],
  ingredients: Seq[String]
)

