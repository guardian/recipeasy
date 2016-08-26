import org.scalatest._
import collection.mutable.Stack
import controllers._
import com.gu.recipeasy.models._
import java.time.OffsetDateTime

class recipeConversion extends FlatSpec with Matchers {
  "A raw recipe" should "be converted to a detailed recipe" in {

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
    val time = OffsetDateTime.now

    val recipe = new Recipe(
      id = "abc",
      title = "Everyday breakfast",
      body = "<p>breakfast</p>",
      serves = None,
      ingredientsLists = breakfast,
      articleId = "breakfast",
      credit = None,
      publicationDate = time,
      status = New,
      steps = None
    )

    val curatedRecipe = new CuratedRecipe(
      id = "abc",
      title = "Everyday breakfast",
      body = "<p>breakfast</p>",
      serves = None,
      ingredientsLists = detailedBreakfast,
      articleId = "breakfast",
      credit = None,
      publicationDate = time,
      status = New,
      times = None,
      steps = None,
      tags = None
    )

    recipeTypeConversion.transformRecipe(recipe) should be(curatedRecipe)
  }
}

