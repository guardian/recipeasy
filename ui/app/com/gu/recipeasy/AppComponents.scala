<<<<<<< HEAD
import auth.{GoogleGroupsAuthorisation, GoogleGroupsAuthorisationDummy}
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, InstanceProfileCredentialsProvider}
import com.amazonaws.regions.{Region, Regions}
import com.gu.cm.{ConfigurationLoader, Identity}
import com.gu.contentapi.client.GuardianContentClient
import com.gu.recipeasy.db.{ContextWrapper, DB}
=======
import auth.{ GoogleGroupsAuthorisation, GoogleGroupsAuthorisationDummy }
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{ AWSCredentialsProviderChain, InstanceProfileCredentialsProvider }
import com.amazonaws.regions.{ Region, Regions }
import com.gu.cm.{ ConfigurationLoader, Identity }
import com.gu.contentapi.client.GuardianContentClient
import com.gu.recipeasy.db.{ ContextWrapper, DB }
>>>>>>> 39d4fb100da8f4a39028cd184f3d8386ca80a2a5
import com.gu.recipeasy.services.ContentApi
import com.gu.recipeasy.{KinesisAppenderConfig, LogStash}
import controllers._
import play.api.ApplicationLoader.Context
<<<<<<< HEAD
import play.api.BuiltInComponentsFromContext
import play.api.i18n.{DefaultLangs, DefaultMessagesApi, MessagesApi}
import play.api.libs.ws.ahc.AhcWSComponents
=======
import play.api.i18n.{ DefaultLangs, DefaultMessagesApi, MessagesApi }
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.BuiltInComponentsFromContext
>>>>>>> 39d4fb100da8f4a39028cd184f3d8386ca80a2a5
import play.api.routing.Router
import play.filters.csrf.CSRFComponents
import play.filters.gzip.GzipFilterComponents
import router.Routes
<<<<<<< HEAD
import schedule.DBHouseKeepingScheduler
import services.{PublisherConfig, _}

import scala.concurrent.Future
=======
import scala.concurrent.Future
import schedule.DBHouseKeepingScheduler
import controllers._
import services._
import services.PublisherConfig
>>>>>>> 39d4fb100da8f4a39028cd184f3d8386ca80a2a5

class AppComponents(context: Context)
    extends BuiltInComponentsFromContext(context)
    with AhcWSComponents
    with GzipFilterComponents
    with CSRFComponents {

  val identity = {
    import com.gu.cm.PlayImplicits._
    Identity.whoAmI("recipeasy", context.environment.mode)
  }

  val credentialsProvider = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider("capi"),
    new InstanceProfileCredentialsProvider()
  )

  val region = Region.getRegion(Regions.fromName(configuration.getString("aws.region").getOrElse(Regions.EU_WEST_1.getName)))

  val appenderConfig = KinesisAppenderConfig(configuration.getString("aws.logging.kinesisStreamName").getOrElse(""), credentialsProvider, region)

  LogStash.init(appenderConfig, context.environment.mode, identity)

  override lazy val configuration = context.initialConfiguration ++ ConfigurationLoader.playConfig(identity, context.environment.mode)
  override lazy val httpFilters = Seq(csrfFilter, gzipFilter)

  val contextWrapper = new ContextWrapper { val config = configuration.underlying }

  applicationLifecycle.addStopHook(() => Future.successful(contextWrapper.dbContext.close()))
  val db = new DB(contextWrapper)
  val messagesApi: MessagesApi = new DefaultMessagesApi(environment, configuration, new DefaultLangs(configuration))
  val dbHouseKeepingScheduler = new DBHouseKeepingScheduler(db)

  val teleporter = new Teleporter(wsClient)

  val googleGroupsAuthorizer = if (context.environment.mode == play.api.Mode.Prod) { new GoogleGroupsAuthorisation(configuration) } else { new GoogleGroupsAuthorisationDummy() }

  val healthcheckController = new Healthcheck
<<<<<<< HEAD

  val applicationController = new Application(wsClient, configuration, db, messagesApi)


  val publisherConfig = PublisherConfig(configuration, region, identity.stage)
  val contentApiClient = new ContentApi(contentApiClient = new GuardianContentClient(publisherConfig.contentAtomConfig.capiKey))

=======

  val publisherConfig = PublisherConfig(configuration, region, identity.stage)
  val contentApiClient = new ContentApi(contentApiClient = new GuardianContentClient(publisherConfig.contentAtomConfig.capiKey))

  val applicationController = new Application(wsClient, configuration, db, messagesApi, publisherConfig, teleporter, contentApiClient)

>>>>>>> 39d4fb100da8f4a39028cd184f3d8386ca80a2a5
  val publisherController = new Publisher(wsClient, configuration, publisherConfig, db, teleporter, contentApiClient)

  val loginController = new Login(wsClient, configuration)
  val adminController = new Admin(wsClient, configuration, db, messagesApi, googleGroupsAuthorizer)

  val assets = new Assets(httpErrorHandler)
  val router: Router = new Routes(httpErrorHandler, healthcheckController, applicationController, publisherController, adminController, loginController, assets)

}
