package controllers

import play.api.mvc._
import play.api.Configuration
import play.api.libs.ws.WSClient
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.i18n.{ I18nSupport, MessagesApi }
import java.time.OffsetDateTime

import com.gu.recipeasy.auth.AuthActions
import com.gu.recipeasy.db._
import com.gu.recipeasy.models._
import com.gu.recipeasy.views
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
    }, { r =>
      val recipe = fromForm(r)
      db.insertCuratedRecipe(recipe)
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
  import TagHelper._
  import Forms._

  case class CuratedRecipeForm(
    id: String,
    title: String,
    body: String,
    serves: Option[Serves],
    ingredientsLists: Seq[DetailedIngredientsList],
    articleId: String,
    credit: Option[String],
    publicationDate: String,
    status: String,
    times: TimesInMins,
    steps: Seq[String],
    tags: FormTags
  )

  def toForm(r: CuratedRecipe): CuratedRecipeForm = {

    val tags = FormTags(r.tags)
    transform[CuratedRecipe, CuratedRecipeForm](
      r,
      "ingredientsLists" -> r.ingredientsLists.lists,
      "publicationDate" -> r.publicationDate.toString,
      "status" -> r.status.toString,
      "steps" -> r.steps.steps,
      "tags" -> FormTags(r.tags)
    )
  }

  def fromForm(r: CuratedRecipeForm): CuratedRecipe = {
    val cuisineTags = getTags(r.tags.cuisine, "cuisine")
    val mealTypeTags = getTags(r.tags.mealType, "mealType")
    val holidayTags = getTags(r.tags.holiday, "holiday")
    val dietaryTags = getDietaryTags(r.tags.dietary)

    transform[CuratedRecipeForm, CuratedRecipe](
      r,
      "ingredientsLists" -> DetailedIngredientsLists(r.ingredientsLists),
      "publicationDate" -> OffsetDateTime.parse(r.publicationDate),
      "status" -> Curated,
      "steps" -> Steps(r.steps),
      "tags" -> Tags(cuisineTags ++ mealTypeTags ++ holidayTags ++ dietaryTags)
    )

  }

  val createCuratedRecipeForm: Form[CuratedRecipeForm] = Form(
    mapping(
      "id" -> of[String],
      "title" -> nonEmptyText(maxLength = 200),
      "body" -> of[String],
      "serves" -> optional(mapping(
        "from" -> number(min = 1),
        "to" -> number(min = 1)
      )(Serves.apply)(Serves.unapply)),
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
      "articleId" -> of[String],
      "credit" -> optional(text(maxLength = 200)),
      "publicationDate" -> of[String],
      "status" -> of[String],
      "times" -> mapping(
        "preparation" -> optional(of[Double]),
        "cooking" -> optional(of[Double])
      )(TimesInMins.apply)(TimesInMins.unapply),
      "steps" -> seq(text),
      "tags" -> mapping(
        "cuisine" -> seq(text),
        "mealType" -> seq(text),
        "holiday" -> seq(text),
        "dietary" -> mapping(
          "lowSugar" -> boolean,
          "lowFat" -> boolean,
          "highFibre" -> boolean,
          "nutFree" -> boolean,
          "glutenFree" -> boolean,
          "dairyFree" -> boolean,
          "eggFree" -> boolean,
          "vegetarian" -> boolean,
          "vegan" -> boolean
        )(Dietary.apply)(Dietary.unapply)
      )(FormTags.apply)(FormTags.unapply)
    )(CuratedRecipeForm.apply)(CuratedRecipeForm.unapply)
  )
}

object TagHelper {

  case class FormTags(
    cuisine: Seq[String],
    mealType: Seq[String],
    holiday: Seq[String],
    dietary: Dietary
  )

  object FormTags {
    def apply(tags: Tags): FormTags = {
      FormTags(
        cuisine = tags.list.collect { case t if t.category == "cuisine" => t.name },
        mealType = tags.list.collect { case t if t.category == "mealType" => t.name },
        holiday = tags.list.collect { case t if t.category == "holiday" => t.name },
        dietary = Dietary(tags)
      )
    }
  }

  case class Dietary(
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

  object Dietary {
    def apply(tags: Tags): Dietary = {
      Dietary(
        lowSugar = tags.list.contains(Tag.lowSugar),
        lowFat = tags.list.contains(Tag.lowFat),
        highFibre = tags.list.contains(Tag.highFibre),
        nutFree = tags.list.contains(Tag.nutFree),
        glutenFree = tags.list.contains(Tag.glutenFree),
        dairyFree = tags.list.contains(Tag.dairyFree),
        eggFree = tags.list.contains(Tag.eggFree),
        vegetarian = tags.list.contains(Tag.vegetarian),
        vegan = tags.list.contains(Tag.vegan)
      )
    }
  }

  def getTags(tags: Seq[String], cat: String): Seq[Tag] = {
    tags.collect { case s: String if (!s.isEmpty) => Tag(s, cat) }
  }

  //TODO make nicer
  def getDietaryTags(t: Dietary): Seq[Tag] = {
    val tags = Map(
      Tag.lowSugar -> t.lowSugar,
      Tag.lowFat -> t.lowFat,
      Tag.highFibre -> t.highFibre,
      Tag.nutFree -> t.nutFree,
      Tag.glutenFree -> t.glutenFree,
      Tag.dairyFree -> t.dairyFree,
      Tag.eggFree -> t.eggFree,
      Tag.vegetarian -> t.vegetarian,
      Tag.vegan -> t.vegan
    )

    tags.filter { _._2 == true }.keySet.toSeq
  }
}

