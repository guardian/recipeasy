import play.api.ApplicationLoader.Context
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.BuiltInComponentsFromContext
import play.api.routing.Router
import controllers._
import play.api.i18n.I18nComponents
import router.Routes

class AppComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with AhcWSComponents
    with I18nComponents {

  val healthcheckController = new Healthcheck
  val applicationController = new Application
  val loginController = new Login(wsClient, configuration)
  val assets = new Assets(httpErrorHandler)
  val router: Router = new Routes(httpErrorHandler, healthcheckController, applicationController, loginController, assets)

}