package etl

import org.jsoup.nodes._

case class RawRecipe(
  title: String, 
  body: Seq[Element]
)

case class ParsedRecipe(
  raw: RawRecipe,
  serves: Option[Serves] = None,
  ingredientsLists: Seq[IngredientList] = Nil,
  image: Option[String] = None
)

case class Serves(from: Int, to: Int)

case class IngredientList(
  title: Option[String],
  ingredients: Seq[String]
)

