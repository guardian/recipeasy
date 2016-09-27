import com.gu.cm.{ ConfigurationLoader, Identity }
import play.filters.csrf.{ CSRFComponents }
import play.api.ApplicationLoader.Context
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.BuiltInComponentsFromContext
import play.api.routing.Router
import play.api.i18n.{ DefaultLangs, DefaultMessagesApi, MessagesApi }
import controllers._
import router.Routes
import io.getquill._
import com.gu.recipeasy.db.DB

import scala.concurrent.Future
import schedule.DBCleaner

class AppComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with AhcWSComponents
    with CSRFComponents {

  val identity = {
    import com.gu.cm.PlayImplicits._
    Identity.whoAmI("recipeasy", context.environment.mode)
  }

  override lazy val configuration = context.initialConfiguration ++ ConfigurationLoader.playConfig(identity, context.environment.mode)
  override lazy val httpFilters = Seq(csrfFilter)

  val dbContext = new JdbcContext[PostgresDialect, SnakeCase](configuration.underlying.getConfig("db.ctx"))
  applicationLifecycle.addStopHook(() => Future.successful(dbContext.close()))
  val db = new DB(dbContext)
  val messagesApi: MessagesApi = new DefaultMessagesApi(environment, configuration, new DefaultLangs(configuration))
  val dbScheduler = new DBCleaner(db)

  val healthcheckController = new Healthcheck
  val applicationController = new Application(wsClient, configuration, db, messagesApi)
  val loginController = new Login(wsClient, configuration)
  val assets = new Assets(httpErrorHandler)
  val router: Router = new Routes(httpErrorHandler, healthcheckController, applicationController, loginController, assets)

}
