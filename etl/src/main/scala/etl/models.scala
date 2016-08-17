package etl

import org.jsoup.nodes._
import com.gu.recipeasy.models._

case class RawRecipe(
  title: String,
  body: Seq[Element]
)

case class ParsedRecipe(
  title: String,
  body: String,
  serves: Option[Serves],
  ingredientsLists: Seq[IngredientsList],
  steps: Seq[String]
)

