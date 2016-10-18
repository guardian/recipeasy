import com.amazonaws.auth.{ AWSCredentialsProviderChain, InstanceProfileCredentialsProvider }
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.{ Region, Regions }
import com.gu.cm.{ ConfigurationLoader, Identity }
import com.gu.recipeasy.{ KinesisAppenderConfig, LogStash }
import play.filters.csrf.CSRFComponents
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
import schedule.DBHouseKeepingScheduler

class AppComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with AhcWSComponents
    with CSRFComponents {

  val identity = {
    import com.gu.cm.PlayImplicits._
    Identity.whoAmI("recipeasy", context.environment.mode)
  }

  val credentialsProvider = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider("capi"),
    new InstanceProfileCredentialsProvider()
  )
  val region = Region getRegion Regions.fromName(configuration.getString("aws.region").getOrElse(Regions.EU_WEST_1.getName))

  val appenderConfig = KinesisAppenderConfig(configuration.underlying.getString("aws.logging.kinesisStreamName"), credentialsProvider, region)

  LogStash.init(appenderConfig, context.environment.mode, identity)

  override lazy val configuration = context.initialConfiguration ++ ConfigurationLoader.playConfig(identity, context.environment.mode)
  override lazy val httpFilters = Seq(csrfFilter)

  val dbContext = new JdbcContext[PostgresDialect, SnakeCase](configuration.underlying.getConfig("db.ctx"))
  applicationLifecycle.addStopHook(() => Future.successful(dbContext.close()))
  val db = new DB(dbContext)
  val messagesApi: MessagesApi = new DefaultMessagesApi(environment, configuration, new DefaultLangs(configuration))
  val dbHouseKeepingScheduler = new DBHouseKeepingScheduler(db)

  val healthcheckController = new Healthcheck
  val applicationController = new Application(wsClient, configuration, db, messagesApi)
  val loginController = new Login(wsClient, configuration)
  val assets = new Assets(httpErrorHandler)
  val router: Router = new Routes(httpErrorHandler, healthcheckController, applicationController, loginController, assets)

}
