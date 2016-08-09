package etl

import java.nio.file._
import org.scalatest._

class RecipeExtractionSpec extends FlatSpec with Matchers {

  def resourceToString(path: String) = new String(Files.readAllBytes(Paths.get(getClass.getClassLoader.getResource(path).toURI())))

    
  it should "cake-doughnut-with-blackberry-glaze-recipe-claire-ptak-baking-the-seasons" in {
    val bodyHtml = resourceToString("articles/cake-doughnut-with-blackberry-glaze-recipe-claire-ptak-baking-the-seasons.txt")
    val recipes = RecipeExtraction.findRecipes("article title", bodyHtml)
    recipes.map(_.title) should be (Seq(
      "Frozen blackberry-geranium cream", 
      "Blackberry cake doughnuts"
    ))
  }

  it should "family-life-my-theatrical-grandfather-coldplay-and-ivf-lentil-pate-for-breakfast" in {
    val bodyHtml = resourceToString("articles/family-life-my-theatrical-grandfather-coldplay-and-ivf-lentil-pate-for-breakfast.txt")
    val recipes = RecipeExtraction.findRecipes("article title", bodyHtml)
    recipes.map(_.title) should be (Seq(
      "We love to eat: Lentil paté for breakfast"
    ))
  }

  it should "banh-xeo-dairy-free-recipe-vietnamese-pancake" in {
    val bodyHtml = resourceToString("articles/banh-xeo-dairy-free-recipe-vietnamese-pancake.txt")
    val recipes = RecipeExtraction.findRecipes("Banh xeo – dairy-free recipe", bodyHtml)
    recipes.map(_.title) should be (Seq(
      "Banh xeo – dairy-free recipe"
    ))
  }

}
