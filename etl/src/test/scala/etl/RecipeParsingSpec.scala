package etl

import java.nio.file._
import org.scalatest._
import org.jsoup.Jsoup
import scala.collection.JavaConverters._

class RecipeParsingSpec extends FlatSpec with Matchers {

  it should "find the serving count" in {
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

    RecipeParsing.guessServes(body("Serves 6")) should be(Some(6))
    RecipeParsing.guessServes(body("(Serves 4)")) should be(Some(4))
    RecipeParsing.guessServes(body("Serves one")) should be(Some(1))
    RecipeParsing.guessServes(body("yolo")) should be(None)
  }

}
