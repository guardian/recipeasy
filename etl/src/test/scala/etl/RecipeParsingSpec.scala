package etl

import java.nio.file._
import org.scalatest._
import org.jsoup.Jsoup
import scala.collection.JavaConverters._
import TestUtils._

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

}
