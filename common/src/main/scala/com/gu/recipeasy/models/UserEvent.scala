package com.gu.recipeasy.models

import automagic._
import java.util.Calendar
import java.time.OffsetDateTime

sealed trait OperationType

case object Curation extends OperationType {
  override def toString: String = "Curation"
}
case object Verification extends OperationType {
  override def toString: String = "Verification"
}
case object Confirmation extends OperationType {
  override def toString: String = "Confirmation"
}
case object AccessRecipeReadOnlyPage extends OperationType {
  override def toString: String = "Recipe Read Only Page"
}
case object AccessRecipeCurationPage extends OperationType {
  override def toString: String = "Access Curation Page"
}
case object AccessRecipeVerificationPage extends OperationType {
  override def toString: String = "Access Verification Page"
}

case class UserEvent(
  user_email: String,
  user_firstname: String,
  user_lastname: String,
  recipe_id: String,
  operation_type: String
)

object UserEvent {

  def toUserEvents(event: UserEvent): UserEventDB = {
    transform[UserEvent, UserEventDB](
      event,
      "event_datetime" -> Calendar.getInstance().getTime()
    )
  }

}

case class UserEventDB(
  event_datetime: java.util.Date,
  user_email: String,
  user_firstname: String,
  user_lastname: String,
  recipe_id: String,
  operation_type: String
)

