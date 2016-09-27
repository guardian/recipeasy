package schedule

import com.gu.recipeasy.db.DB
import org.quartz.JobBuilder._
import org.quartz.TriggerBuilder._
import org.quartz.CronScheduleBuilder._
import org.quartz.{ Job, JobDataMap, JobExecutionContext }
import org.quartz.impl.StdSchedulerFactory

class DBHouseKeepingScheduler(db: DB) {

  private val scheduler = StdSchedulerFactory.getDefaultScheduler()

  private def buildJobDataMap: JobDataMap = {
    val map = new JobDataMap()
    map.put("DB", db)
    map
  }

  private val jobDetail = newJob(classOf[HouseKeepingJob])
    .withIdentity("cleanDB")
    .usingJobData(buildJobDataMap)
    .build()

  private val trigger = newTrigger()
    .withIdentity("cleanDB")
    .withSchedule(dailyAtHourAndMinute(0, 0))
    .build()

  scheduler.scheduleJob(jobDetail, trigger)

  def start(): Unit = scheduler.start()
  def shutdown(): Unit = scheduler.shutdown()

}

class HouseKeepingJob extends Job {
  override def execute(context: JobExecutionContext): Unit = {
    val jobDataMap = context.getJobDetail.getJobDataMap
    val db = jobDataMap.get("DB").asInstanceOf[DB]
    db.resetStatus()
  }
}
