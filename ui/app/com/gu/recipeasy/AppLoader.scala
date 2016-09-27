import play.api.libs.logback.LogbackLoggerConfigurator
import play.api.{ Application, ApplicationLoader }
import play.api.ApplicationLoader.Context

import scala.concurrent.Future

class AppLoader extends ApplicationLoader {
  override def load(context: Context): Application = {
    new LogbackLoggerConfigurator().configure(context.environment)
    val components = new AppComponents(context)

    components.dbHouseKeepingScheduler.start
    components.applicationLifecycle.addStopHook { () =>
      println("Shutting down scheduler")
      Future.successful(components.dbHouseKeepingScheduler.shutdown())
    }

    components.application
  }
}