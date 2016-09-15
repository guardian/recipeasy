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
      case Some(r) => Ok(views.html.recipeLayout(createCuratedRecipeForm.fill(toForm(recipeTypeConversion.transformRecipe(r)))))
      case None => NotFound
    }
  }

  def curateRecipe = Action { implicit request =>
    //create curated recipe to store in db
    val formValidationResult = Application.createCuratedRecipeForm.bindFromRequest
    formValidationResult.fold({ formWithErrors =>
      BadRequest(views.html.recipeLayout(formWithErrors))
    }, { recipe =>
      //TODO need to convert model to a db model first
      //db.insertCuratedRecipe(recipe)
      Redirect(routes.Application.curateRecipePage)
    })
  }

}

object recipeTypeConversion {
  def transformRecipe(r: Recipe): CuratedRecipe = {
    transform[Recipe, CuratedRecipe](
      r,
      "times" -> TimesInMins(None, None),
      "tags" -> Tags(List.empty),
      "ingredientsLists" -> rawToDetailedIngredientsLists(r.ingredientsLists)
    )
  }

  def rawToDetailedIngredientsLists(ingredients: IngredientsLists): DetailedIngredientsLists = {
    new DetailedIngredientsLists(lists =
      ingredients.lists.map(r => new DetailedIngredientsList(r.title, rawToDetailedIngredients(r.ingredients))))
  }

  def rawToDetailedIngredients(ingredients: Seq[String]): Seq[DetailedIngredient] = {
    ingredients.map(i => new DetailedIngredient(None, None, "", None, i))
  }

}

object Application {
  import Forms._

  case class CuratedRecipeForm(
    title: String,
    servesFrom: Option[Int],
    servesTo: Option[Int],
    credit: Option[String],
    ingredientsLists: Seq[DetailedIngredientsList],
    status: String,
    timePreparation: Option[Double],
    timeCooking: Option[Double],
    steps: Seq[String],
    tags: FormTags
  )

  case class FormTags(
    cuisine: Seq[String],
    mealType: Seq[String],
    holiday: Seq[String],
    diets: Diet
  )

  case class Diet(
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
      ingredientsLists = r.ingredientsLists.lists,
      status = r.status.toString,
      timePreparation = r.times.preparation,
      timeCooking = r.times.cooking,
      steps = r.steps.steps,
      tags = FormTags(
        cuisine = r.tags.list.collect { case t if t.category == "cuisine" => t.name },
        mealType = r.tags.list.collect { case t if t.category == "mealType" => t.name },
        holiday = r.tags.list.collect { case t if t.category == "holiday" => t.name },
        diets = Diet(
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
      )
    )
  }

  val createCuratedRecipeForm: Form[CuratedRecipeForm] = Form(
    mapping(
      "title" -> nonEmptyText(maxLength = 200),
      "servesFrom" -> optional(number(min = 1)),
      "servesTo" -> optional(number(min = 1)),
      "credit" -> optional(text(maxLength = 200)),
      "ingredientsLists" -> seq(mapping(
        "title" -> optional(nonEmptyText(maxLength = 200)),
        "ingredients" -> seq(mapping(
          "quantity" -> optional(of[Double]),
          "unit" -> optional(text.transform[CookingUnit](CookingUnit.fromString(_).getOrElse(Handful), _.abbreviation)),
          "item" -> nonEmptyText(maxLength = 200),
          "comment" -> optional(text(maxLength = 200)),
          "raw" -> text
        )(DetailedIngredient.apply)(DetailedIngredient.unapply))
      )(DetailedIngredientsList.apply)(DetailedIngredientsList.unapply)),
      "status" -> text,
      "timePreparation" -> optional(of[Double]),
      "timeCooking" -> optional(of[Double]),
      "steps" -> seq(text),
      "tags" -> mapping(
        "cuisine" -> seq(text),
        "mealType" -> seq(text),
        "holiday" -> seq(text),
        "diet" -> mapping(
          "lowSugar" -> boolean,
          "lowFat" -> boolean,
          "highFibre" -> boolean,
          "nutFree" -> boolean,
          "glutenFree" -> boolean,
          "dairyFree" -> boolean,
          "eggFree" -> boolean,
          "vegetarian" -> boolean,
          "vegan" -> boolean
        )(Diet.apply)(Diet.unapply)
      )(FormTags.apply)(FormTags.unapply)
    )(CuratedRecipeForm.apply)(CuratedRecipeForm.unapply)
  )
}

