package controllers

import com.gu.recipeasy.auth.AuthActions
import play.api.mvc._
import com.gu.recipeasy.views
import play.api.Configuration
import play.api.libs.ws.WSClient

class Application(override val wsClient: WSClient, override val conf: Configuration) extends Controller with AuthActions {
  def index = AuthAction {
    Ok(views.html.app("Recipeasy"))
  }
}
