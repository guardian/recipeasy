package com.gu.recipeasy.models

import automagic._
import java.util.Calendar
import java.time.OffsetDateTime

import com.gu.recipeasy.db._

sealed trait GeneralStatisticsPoint

case object GStatsUserParticipationCount extends GeneralStatisticsPoint
case object GStatsCuratedRecipesCount extends GeneralStatisticsPoint
case object GStatsFinalisedRecipesCount extends GeneralStatisticsPoint
case object GStatsTotalActiveRecipesCount extends GeneralStatisticsPoint

sealed trait PersonalStatisticsPoint
case object PStatsCurationCount extends PersonalStatisticsPoint
case object PStatsVerificationCount extends PersonalStatisticsPoint
case object PStatsFinalisationCount extends PersonalStatisticsPoint
case object PStatsBiggestDayDate extends PersonalStatisticsPoint
case object PStatsBiggestDayCount extends PersonalStatisticsPoint
case object PStatsRanking extends PersonalStatisticsPoint

case class DayActivityDistribution(date: String, curationCount: Int, verificationCount: Int, confirmationCount: Int)

case class LeaderboardEntry(userEmail: String, curationCount: Int, verificationCount: Int, confirmationCount: Int) {
  def total: Int = curationCount + verificationCount + confirmationCount
}

object Leaderboard {
  def eventsToOrderedLeaderboardEntries(events: List[UserEventDB]): List[LeaderboardEntry] = {

    def eventsForEmailAddress(events: List[UserEventDB], email: String): List[UserEventDB] = {
      events.filter(event => event.user_email == email)
    }

    def eventsToCurationNumber(events: List[UserEventDB]): Int = {
      events.filter(event => event.operation_type == UserEventCuration.name).size
    }

    def eventsToVerificationNumber(events: List[UserEventDB]): Int = {
      events.filter(event => event.operation_type == UserEventVerification.name).size
    }

    def eventsToConfirmationNumber(events: List[UserEventDB]): Int = {
      events.filter(event => event.operation_type == UserEventConfirmation.name).size
    }

    events.map(event => event.user_email).distinct.map { email =>
      val userEvents = eventsForEmailAddress(events, email)
      LeaderboardEntry(email, eventsToCurationNumber(userEvents), eventsToVerificationNumber(userEvents), eventsToConfirmationNumber(userEvents))
    }.sortWith((le1, le2) => le1.total < le2.total).reverse

  }
}

case class UserSpeeds(curation: Double, verification: Double)

object UsersSpeedsMeasurements {

  def average(numbers: List[Double]): Double = if (numbers.nonEmpty) { numbers.sum / numbers.size } else { 0.toDouble }

  def secondsBetweenEvents(eventStart: UserEventDB, eventEnd: UserEventDB): Double = (eventEnd.event_datetime.getTime - eventStart.event_datetime.getTime).toDouble / 1000 // converting to seconds

  def pairOfEventsIsNormalised(events: (UserEventDB, UserEventDB), firstOperationName: String, secondOperationName: String): Boolean = events._1.operation_type == firstOperationName && events._2.operation_type == secondOperationName && events._1.recipe_id == events._2.recipe_id

  def generalUserSpeedMapping(db: DB): Map[String, UserSpeeds] = {
    val userEventsAll: List[UserEventDB] = db.userEventsAll().reverse
    db.userEmails().map { email =>

      val userEvents = userEventsAll.filter(event => event.user_email == email)

      val curationEvents = userEvents.filter(event => (event.operation_type == UserEventAccessRecipeCurationPage.name || event.operation_type == UserEventCuration.name))
      val curationTimings: List[Double] = if (curationEvents.nonEmpty) {
        curationEvents.zip(curationEvents.tail)
          .filter(events => pairOfEventsIsNormalised(events, UserEventAccessRecipeCurationPage.name, UserEventCuration.name))
          .map(events => secondsBetweenEvents(events._1, events._2))
          .filter(timing => timing <= 1200)
      } else {
        Nil
      }

      val verificationEvents = userEvents.filter(event => (event.operation_type == UserEventAccessRecipeVerificationPage.name || event.operation_type == UserEventVerification.name))
      val verificationTimings: List[Double] = if (verificationEvents.nonEmpty) {
        verificationEvents.zip(verificationEvents.tail)
          .filter(events => pairOfEventsIsNormalised(events, UserEventAccessRecipeVerificationPage.name, UserEventVerification.name))
          .map(events => secondsBetweenEvents(events._1, events._2))
          .filter(timing => timing <= 1200)
      } else {
        Nil
      }

      (
        email,
        UserSpeeds(
          average(curationTimings),
          average(verificationTimings)
        )
      )

    }.toMap
  }
}
