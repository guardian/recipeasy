package etl

import org.scalatest._
import TestUtils._

class RecipeExtractionSpec extends FunSuite with Matchers {

  test("cake-doughnut-with-blackberry-glaze-recipe-claire-ptak-baking-the-seasons") {
    val bodyHtml = resourceToString("articles/cake-doughnut-with-blackberry-glaze-recipe-claire-ptak-baking-the-seasons.txt")
    val recipes = RecipeExtraction.findRecipes("article title", bodyHtml)
    recipes.map(_.title) should be(Seq(
      "Frozen blackberry-geranium cream",
      "Blackberry cake doughnuts"
    ))
  }

  test("family-life-my-theatrical-grandfather-coldplay-and-ivf-lentil-pate-for-breakfast") {
    val bodyHtml = resourceToString("articles/family-life-my-theatrical-grandfather-coldplay-and-ivf-lentil-pate-for-breakfast.txt")
    val recipes = RecipeExtraction.findRecipes("article title", bodyHtml)
    recipes.map(_.title) should be(Seq(
      "We love to eat: Lentil paté for breakfast"
    ))
  }

  test("banh-xeo-dairy-free-recipe-vietnamese-pancake") {
    val bodyHtml = resourceToString("articles/banh-xeo-dairy-free-recipe-vietnamese-pancake.txt")
    val recipes = RecipeExtraction.findRecipes("Banh xeo – dairy-free recipe", bodyHtml)
    recipes.map(_.title) should be(Seq(
      "Banh xeo – dairy-free recipe"
    ))
  }

  test("peach-raspberry-recipes-belly-pork-salad-cake-stuffed-yotam-ottolenghi") {
    val bodyHtml = resourceToString("articles/peach-raspberry-recipes-belly-pork-salad-cake-stuffed-yotam-ottolenghi.txt")
    val recipes = RecipeExtraction.findRecipes("article title", bodyHtml)
    recipes.map(_.title) should be(Seq(
      "Five-spice pork belly with peach, raspberry and watercress salad",
      "Raspberry- and almond-stuffed peach with amaretto sabayon",
      "Hazelnut, peach and raspberry cake"
    ))
  }

  test("2002-aug-31-foodanddrink.shopping") {
    // This is an example of a really old article that uses <strong> instead of <h2>
    val bodyHtml = resourceToString("articles/2002-aug-31-foodanddrink.shopping.txt")
    val recipes = RecipeExtraction.findRecipes("article title", bodyHtml)

    pending
    // Currently doesn't work because this article uses "<p><strong>short text</strong><br></p>" 
    // for recipe titles (and also for ingredients). The <br> throws off the matcher. orz

    recipes.map(_.title) should be(Seq(
      "Onion purée",
      "Malik's onion bhajee",
      "Dried onion slices"
    ))
  }
}
