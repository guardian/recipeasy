package etl

import java.nio.file._
import org.scalatest._

class RecipeExtractionSpec extends FlatSpec with Matchers {

  def resourceToString(path: String) = new String(Files.readAllBytes(Paths.get(getClass.getClassLoader.getResource(path).toURI())))

  val bodyHtml = resourceToString("articles/cake-doughnut-with-blackberry-glaze-recipe-claire-ptak-baking-the-seasons.txt")
    
  it should "separate a multi-recipe article into raw recipes" in {
    val recipes = RecipeExtraction.findRecipes("article title", bodyHtml)
    recipes.map(_.title) should be (Seq(
      "Frozen blackberry-geranium cream", 
      "Blackberry cake doughnuts"
    ))
  }

}
