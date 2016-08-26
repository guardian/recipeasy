import org.scalatest._
import collection.mutable.Stack
import controllers._
import com.gu.recipeasy.models._

class recipeConversion extends FlatSpec with Matchers {
  "A raw ingredientis list" should "be converted to a detailed ingredients list" in {
    val toast = new IngredientsList(Some("toast"), Seq("bread", "toaster"))
    val porridge = new IngredientsList(Some("porridge"), Seq("cup of oats", "glug of milk", "banana"))
    val bread = new DetailedIngredient(None, None, None, None, "bread")
    val toaster = new DetailedIngredient(None, None, None, None, "toaster")
    val oats = new DetailedIngredient(None, None, None, None, "cup of oats")
    val milk = new DetailedIngredient(None, None, None, None, "glug of milk")
    val banana = new DetailedIngredient(None, None, None, None, "banana")

    val detailedToast = new DetailedIngredientsList(Some("toast"), Seq(bread, toaster))
    val detailedPorridge = new DetailedIngredientsList(Some("porridge"), Seq(oats, milk, banana))
    val breakfast = new IngredientsLists(Seq(toast, porridge))
    val detailedBreakfast = new DetailedIngredientsLists(Seq(detailedToast, detailedPorridge))

    recipeTypeConversion.rawToDetailedIngredientsLists(breakfast) should be(detailedBreakfast)
  }
}

