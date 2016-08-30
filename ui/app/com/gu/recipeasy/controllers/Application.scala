package controllers

import com.gu.recipeasy.auth.AuthActions
import play.api.mvc._
import com.gu.recipeasy.views
import play.api.Configuration
import play.api.libs.ws.WSClient
import play.api.data.Forms._
import play.api.data._
import play.api.data.format.Formats._
import play.api.i18n.{ I18nSupport, MessagesApi }
import com.gu.recipeasy.db._
import com.gu.recipeasy.models._
import automagic._

class Application(override val wsClient: WSClient, override val conf: Configuration, db: DB, val messagesApi: MessagesApi) extends Controller with AuthActions with I18nSupport {
  import Forms._
  import Application._

  def index = AuthAction {
    Ok(views.html.app(("Title")))
  }

  def curateRecipePage = Action { implicit request =>
    val newRecipe = db.getNewRecipe
    newRecipe match {
      case Some(r) => Ok(views.html.recipeLayout(toForm(recipeTypeConversion.transformRecipe(r)), createCuratedRecipeForm: Form[CuratedRecipeForm]))
      case None => NotFound
    }
  }

  def curateRecipe = Action { implicit request =>
    //create curated recipe to store in db
    val formValidationResult = Application.createCuratedRecipeForm.bindFromRequest
    formValidationResult.fold({ formWithErrors =>
      BadRequest(views.html.recipeLayout(???, formWithErrors))
    }, { recipe =>
      //TODO need to convert model to a db model first
      //db.insertCuratedRecipe(recipe)
      println("good request")
      Redirect(routes.Application.curateRecipePage)
    })
  }

}

object recipeTypeConversion {
  def transformRecipe(r: Recipe): CuratedRecipe = {
    transform[Recipe, CuratedRecipe](
      r,
      "times" -> Times(None, None),
      "tags" -> Tags(List.empty),
      "ingredientsLists" -> rawToDetailedIngredientsLists(r.ingredientsLists)
    )
  }

  def rawToDetailedIngredientsLists(ingredients: IngredientsLists): DetailedIngredientsLists = {
    new DetailedIngredientsLists(lists =
      ingredients.lists.map(r => new DetailedIngredientsList(r.title, rawToDetailedIngredients(r.ingredients))))
  }

  def rawToDetailedIngredients(ingredients: Seq[String]): Seq[DetailedIngredient] = {
    ingredients.map(i => new DetailedIngredient(None, None, None, None, i))
  }

}

object Application {
  import Forms._

  case class CuratedRecipeForm(
    title: String,
    servesFrom: Option[Int],
    servesTo: Option[Int],
    credit: Option[String],
    ingredients: String,
    status: String,
    timePreparationQuantity: Option[Double],
    timePreparationUnit: Option[String],
    timeCookingQuantity: Option[Double],
    timeCookingUnit: Option[String],
    steps: Seq[String],
    cuisine: Seq[String],
    mealType: Seq[String],
    lowSugar: Boolean,
    lowFat: Boolean,
    highFibre: Boolean,
    nutFree: Boolean,
    glutenFree: Boolean,
    dairyFree: Boolean,
    eggFree: Boolean,
    vegetarian: Boolean,
    vegan: Boolean
  )

  def toForm(r: CuratedRecipe): CuratedRecipeForm = {
    CuratedRecipeForm(
      title = r.title,
      servesFrom = r.serves.map(_.from),
      servesTo = r.serves.map(_.to),
      credit = r.credit,
      ingredients = "A nested form with ingredients",
      status = r.status.toString,
      timePreparationQuantity = r.times.preparation.map(_.quantity),
      timePreparationUnit = r.times.preparation.map(_.unit),
      timeCookingQuantity = r.times.preparation.map(_.quantity),
      timeCookingUnit = r.times.preparation.map(_.unit),
      steps = Seq.empty, //r.steps.steps,
      cuisine = r.tags.list.collect { case t if t.category == "cuisine" => t.name },
      mealType = r.tags.list.collect { case t if t.category == "mealType" => t.name },
      lowSugar = r.tags.list.contains(Tag.lowSugar),
      lowFat = r.tags.list.contains(Tag.lowFat),
      highFibre = r.tags.list.contains(Tag.highFibre),
      nutFree = r.tags.list.contains(Tag.nutFree),
      glutenFree = r.tags.list.contains(Tag.glutenFree),
      dairyFree = r.tags.list.contains(Tag.dairyFree),
      eggFree = r.tags.list.contains(Tag.eggFree),
      vegetarian = r.tags.list.contains(Tag.vegetarian),
      vegan = r.tags.list.contains(Tag.vegan)
    )
  }

  case class IngredientsListsForm(
    ingredientQuantity: Option[String],
    ingredientUnit: Option[String],
    ingredientItem: String,
    ingredientComment: Option[String]
  )

  val createCuratedRecipeForm: Form[CuratedRecipeForm] = Form(
    mapping(
      "title" -> nonEmptyText(maxLength = 200),
      "servesFrom" -> optional(number),
      "servesTo" -> optional(number),
      "credit" -> optional(text(maxLength = 200)),
      "ingredients" -> nonEmptyText(maxLength = 200),
      // "ingredientQuantity" -> optional(text(maxLength = 200)),
      // "ingredientUnit" -> optional(text(maxLength = 200)),
      // "ingredientItem" -> text(maxLength = 200),
      // "ingredientComment" -> optional(text(maxLength = 200)),
      "status" -> nonEmptyText(maxLength = 200),
      "timePreparationQuantity" -> optional(of[Double]),
      "timePreparationUnit" -> optional(text(maxLength = 200)),
      "timeCookingQuantity" -> optional(of[Double]),
      "timeCookingUnit" -> optional(text(maxLength = 200)),
      "steps" -> seq(text),
      "cuisine" -> seq(text(maxLength = 200)),
      "mealType" -> seq(text(maxLength = 200)),
      "lowSugar" -> boolean,
      "lowFat" -> boolean,
      "highFibre" -> boolean,
      "nutFree" -> boolean,
      "glutenFree" -> boolean,
      "dairyFree" -> boolean,
      "eggFree" -> boolean,
      "vegetarian" -> boolean,
      "vegan" -> boolean

    )(CuratedRecipeForm.apply)(CuratedRecipeForm.unapply)
  )
}
