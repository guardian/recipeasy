package com.gu.recipeasy.models

import automagic._
import java.util.Calendar
import java.time.OffsetDateTime

sealed trait OperationType { val name: String }

case object Curation extends OperationType {
  val name = "Curation"
}
case object Verification extends OperationType {
  val name = "Verification"
}
case object Confirmation extends OperationType {
  val name = "Confirmation"
}
case object AccessRecipeReadOnlyPage extends OperationType {
  val name = "Recipe Read Only Page"
}
case object AccessRecipeCurationPage extends OperationType {
  val name = "Access Curation Page"
}
case object AccessRecipeVerificationPage extends OperationType {
  val name = "Access Verification Page"
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

