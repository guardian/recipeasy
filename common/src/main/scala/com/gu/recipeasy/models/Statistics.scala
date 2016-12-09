package com.gu.recipeasy.models

import automagic._
import java.util.Calendar
import java.time.OffsetDateTime

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
  def total(): Int = curationCount + verificationCount + confirmationCount
}

object Leaderboard {
  def eventsToOrderedLeaderboardEntries(events: List[UserEventDB]): List[LeaderboardEntry] = {

    def eventsForEmailAddress(events: List[UserEventDB], email: String): List[UserEventDB] = {
      events.filter(event => event.user_email == email)
    }

    def eventsToCurationNumber(events: List[UserEventDB]): Int = {
      events.filter(event => event.operation_type == Curation.toString).size
    }

    def eventsToVerificationNumber(events: List[UserEventDB]): Int = {
      events.filter(event => event.operation_type == Verification.toString).size
    }

    def eventsToConfirmationNumber(events: List[UserEventDB]): Int = {
      events.filter(event => event.operation_type == Confirmation.toString).size
    }

    events.map(event => event.user_email).distinct.map { email =>
      val userEvents = eventsForEmailAddress(events, email)
      LeaderboardEntry(email, eventsToCurationNumber(userEvents), eventsToVerificationNumber(userEvents), eventsToConfirmationNumber(userEvents))
    }.sortWith((le1, le2) => le1.total() <= le2.total()).reverse

  }
}

