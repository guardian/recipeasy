package controllers

import play.api.mvc._
import com.gu.recipeasy.auth.AuthActions
import com.gu.recipeasy.views

class Application extends Controller {
  def index = Action {
    Ok(views.html.app("Recipeasy"))
  }
}
