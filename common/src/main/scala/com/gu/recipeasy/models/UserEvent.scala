package com.gu.recipeasy.models

import automagic._
import java.util.Calendar
import java.time.OffsetDateTime

import com.gu.recipeasy.db.DB

sealed trait UserEventOperationType { val name: String }

case object UserEventCuration extends UserEventOperationType {
  val name = "Curation"
}
case object UserEventVerification extends UserEventOperationType {
  val name = "Verification"
}
case object UserEventConfirmation extends UserEventOperationType {
  val name = "Confirmation"
}
case object UserEventAccessRecipeReadOnlyPage extends UserEventOperationType {
  val name = "Recipe Read Only Page"
}
case object UserEventAccessRecipeCurationPage extends UserEventOperationType {
  val name = "Access Curation Page"
}
case object UserEventAccessRecipeVerificationPage extends UserEventOperationType {
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

case class CurationUser(emailAddress: String, firstName: String, lastName: String)

object CurationUser {
  def getCurationUser(recipeId: String, db: DB): CurationUser = {
    db.getFirstCurationEventForRecipe(recipeId) match {
      case Some(userEventDB) => CurationUser(userEventDB.user_email, userEventDB.user_firstname, userEventDB.user_lastname)
      case None => CurationUser("off-platform@guardian.co.uk", "off-platform", "")
    }
  }
}