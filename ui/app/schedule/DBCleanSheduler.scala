package schedule

import com.gu.recipeasy.db.DB
import org.quartz.JobBuilder._
import org.quartz.TriggerBuilder._
import org.quartz.CronScheduleBuilder._
import org.quartz.{ Job, JobDataMap, JobExecutionContext }
import org.quartz.impl.StdSchedulerFactory

class DBCleaner(db: DB) {

  private val scheduler = StdSchedulerFactory.getDefaultScheduler()

  private def buildJobDataMap: JobDataMap = {
    val map = new JobDataMap()
    map.put("DB", db.resetStatus)
    map
  }

  val jobDetail = newJob(classOf[HouseKeepingJob])
    .withIdentity("cleanDB")
    .usingJobData(buildJobDataMap)
    .build()

  val trigger = newTrigger()
    .withIdentity("cleanDB")
    .withSchedule(dailyAtHourAndMinute(17, 20))
    .build()

  scheduler.scheduleJob(jobDetail, trigger)

  def start(): Unit = scheduler.start()
  def shutdown(): Unit = scheduler.shutdown()

}

class HouseKeepingJob extends Job {
  override def execute(context: JobExecutionContext): Unit = {
    implicit val jobDataMap = context.getJobDetail.getJobDataMap
  }
}
