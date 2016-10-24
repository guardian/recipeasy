package etl

import java.nio.file._
import org.scalatest._
import org.jsoup.Jsoup
import scala.collection.JavaConverters._
import TestUtils._
import com.gu.recipeasy.models._

class RecipeParsingSpec extends FlatSpec with Matchers {

  def body(serves: String) = {
    val html = s"""
      <p>This is a lovely recipe</p>
      <p>
        <strong>10kg quinoa</strong>
        <br/>
        <strong>A pinch of ethically sourced pixie dust</strong>
      </p>
      <p>$serves</p>
      <p>1. Sprinkle pixie dust over quinoa and serve.</p>
    """
    Jsoup.parse(html).body.children.asScala
  }

  it should "find the serving count" in {
    RecipeParsing.guessServes(body("Serves 6")) should be(Some(Serves(6, 6)))
    RecipeParsing.guessServes(body("(Serves 4)")) should be(Some(Serves(4, 4)))
    RecipeParsing.guessServes(body("Serves one")) should be(Some(Serves(1, 1)))
    RecipeParsing.guessServes(body("yolo")) should be(None)
    RecipeParsing.guessServes(body("serves 8 <br>for the marinade")) should be(Some(Serves(8, 8)))
  }

  it should "find a more complex serving count" in {
    pending
    RecipeParsing.guessServes(body("Serves 4-6")) should be(Some(Serves(4, 6)))
  }

  it should "find the ingredients lists when list titles are <strong> and ingredients are normal text" in {
    val bodyHtml = resourceToString("articles/raw-vegetable-salad-recipes-anna-jones-the-modern-cook.txt")
    val recipes = RecipeExtraction.findRecipes("article title", bodyHtml)
    recipes should have size 4

    val first = RecipeParsing.parseRecipe(recipes(0)) // Carrot and mustard seed salad
    val second = RecipeParsing.parseRecipe(recipes(1)) // Raw cauliflower salad
    val third = RecipeParsing.parseRecipe(recipes(2)) // Late-summer fruits with lime leaves

    first.ingredientsLists should have size 1

    first.ingredientsLists(0).title should be(None)
    first.ingredientsLists(0).ingredients.head should be("50g (a small handful) unsweetened desiccated coconut")
    first.ingredientsLists(0).ingredients.last should be("A small bunch of coriander, roughly chopped")

    second.ingredientsLists should have size 2

    second.ingredientsLists(0).title should be(None)
    second.ingredientsLists(0).ingredients.head should be("1 cucumber, seeded, cut into small pieces")
    second.ingredientsLists(0).ingredients.last should be("A small bunch of flat-leaf parsley, roughly chopped")

    second.ingredientsLists(1).title should be(Some("For the dressing"))
    second.ingredientsLists(1).ingredients.head should be("200g Greek yoghurt")
    second.ingredientsLists(1).ingredients.last should be("Salt and black pepper")

    third.ingredientsLists should have size 1

    third.ingredientsLists(0).title should be(None)
    third.ingredientsLists(0).ingredients.head should be("A stalk of lemongrass")
    third.ingredientsLists(0).ingredients.last should be("Fruit (I used a mixture of white peaches, nectarines, yellow plums and blackberries)")

  }

  it should "find the ingredients lists when list titles are normal text and ingredients are <strong>" in {

    val bodyHtml = resourceToString("articles/peach-raspberry-recipes-belly-pork-salad-cake-stuffed-yotam-ottolenghi.txt")
    val recipes = RecipeExtraction.findRecipes("article title", bodyHtml)
    recipes should have size 3

    val first = RecipeParsing.parseRecipe(recipes(0)) // Five-spice pork belly
    val second = RecipeParsing.parseRecipe(recipes(1)) // Raspberry- and almond-stuffed peach
    val third = RecipeParsing.parseRecipe(recipes(2)) // Hazelnut, peach and raspberry cake

    first.ingredientsLists should have size 2

    first.ingredientsLists(0).title should be(None)
    first.ingredientsLists(0).ingredients.head should be("60ml maple syrup")
    first.ingredientsLists(0).ingredients.last should be("100ml sunflower oil")

    first.ingredientsLists(1).title should be(Some("For the peach and raspberry salad"))
    first.ingredientsLists(1).ingredients.head should be("1½ tbsp cider vinegar")
    first.ingredientsLists(1).ingredients.last should be("½ small radicchio, leaves separated and cut into 2.5cm-wide slices")

    second.ingredientsLists should have size 2

    second.ingredientsLists(0).title should be(None)
    second.ingredientsLists(0).ingredients.head should be("4 large ripe peaches, halved and stoned")
    second.ingredientsLists(0).ingredients.last should be("1 tsp freshly ground nutmeg")

    second.ingredientsLists(1).title should be(Some("For the sabayon"))
    second.ingredientsLists(1).ingredients.head should be("6 egg yolks")
    second.ingredientsLists(1).ingredients.last should be("50g cream cheese, at room temperature")

    third.ingredientsLists should have size 1

    third.ingredientsLists(0).title should be(None)
    third.ingredientsLists(0).ingredients.head should be("2 tsp sunflower oil")
    third.ingredientsLists(0).ingredients.last should be("⅛ tsp salt")

  }

  it should "find the ingredient list title correctly when there is also a serving count" in {

    val bodyHtml = resourceToString("articles/turmeric-recipes-chocolate-mousse-hummus-saag-paneer-recipe-swap.txt")
    val recipes = RecipeExtraction.findRecipes("article title", bodyHtml)
    recipes should have size 6

    val first = RecipeParsing.parseRecipe(recipes(0)) // The winning recipe

    first.ingredientsLists should have size 2

    first.ingredientsLists(0).title should be(Some("For the syrup"))
    first.ingredientsLists(0).ingredients.head should be("30ml water")
    first.ingredientsLists(0).ingredients.last should be("10g fresh turmeric, peeled and finely chopped")

    first.ingredientsLists(1).title should be(Some("For the mousse"))
    first.ingredientsLists(1).ingredients.head should be("175g dark chocolate, broken into pieces")
    first.ingredientsLists(1).ingredients.last should be("Fresh cherries, stoned, to serve")

  }

  it should "find the ingredients list when each ingredient is a separate <p>" in {
    // This is how ingredients are listed in really old articles.
    // But we can't even extract those recipes from the articles yet,
    // so there's no point in being able to parse them.
    pending
  }

  it should "separate the method into steps" in {
    val expectedSteps = Seq(
      "For the syrup, put the water and sugar in a small pan and bring to the boil until the sugar dissolves completely. On a low heat, add the orange peels and turmeric, let it boil for about 5 minutes, then set aside.",

      "Put a heatproof bowl over a pan with simmering water in it, ensuring that the bottom of the bowl doesn’t touch the water. Put the chocolate into the bowl, add your fresh and ground turmeric, as well as the cayenne pepper, and let it all melt together.",

      "Meanwhile, whisk the egg yolks with caster sugar until smooth. In a separate bowl whisk the egg whites with the icing sugar until it’s stiff.",

      "Turn the heat down under the chocolate, add the egg yolks and quickly whisk until combined and the mix has a thick consistency, then add the double cream and continue to whisk.",

      "Next, add the syrup and whisk to combine. Take off the heat and fold in the egg whites, then pour into individual serving bowls. Let it rest in the fridge for 30-60 minutes, add your cherries and serve."
    )

    val bodyHtml = resourceToString("articles/turmeric-recipes-chocolate-mousse-hummus-saag-paneer-recipe-swap.txt")
    val recipes = RecipeExtraction.findRecipes("article title", bodyHtml)

    val first = RecipeParsing.parseRecipe(recipes(0))

    first.steps should be(expectedSteps)

  }

}
