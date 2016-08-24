package controllers

import com.gu.recipeasy.auth.AuthActions
import play.api.mvc._
import com.gu.recipeasy.views
import play.api.Configuration
import play.api.libs.ws.WSClient
import com.gu.recipeasy.db._

class Application(override val wsClient: WSClient, override val conf: Configuration, db: DB) extends Controller with AuthActions {
  def index = AuthAction {
    val newRecipe = db.getNewRecipe
    newRecipe match {
      case Some(r) => Ok(views.html.app(r))
      case None => NotFound
    }
  }
}
