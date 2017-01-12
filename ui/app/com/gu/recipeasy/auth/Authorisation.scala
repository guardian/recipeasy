package auth
import play.api.Configuration
import java.io.FileInputStream

import com.gu.googleauth.{ GoogleGroupChecker, GoogleServiceAccount }
import play.api.{ Configuration, Logger }

import scala.concurrent.{ ExecutionContext, Future }
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential

trait Authorisation {
  def isAdmin(email: String)(implicit ec: ExecutionContext): Future[Boolean]
}

class GoogleGroupsAuthorisation(conf: Configuration) extends Authorisation {

  val serviceAccount = GoogleGroupsAuthorisation.serviceAccount(conf)

  private val checker = new GoogleGroupChecker(serviceAccount)

  private def getGroupsForUser(email: String)(implicit ec: ExecutionContext): Future[Set[String]] = {
    //checker.retrieveGroupsFor(email).map { groups =>
    //  Logger.info(s"User $email is in the following groups: $groups")
    //  groups
    //}
    Future.successful(Set())
  }

  def isAdmin(email: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    Future.successful(true)
  }

}

class GoogleGroupsAuthorisationDummy() extends Authorisation {
  def isAdmin(email: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    Future.successful(true)
  }
}

object GoogleGroupsAuthorisation {
  def serviceAccount(configuration: Configuration) = {
    val certFile = new FileInputStream("/etc/gu/recipeasy-service-account.json")
    val credential = GoogleCredential.fromStream(certFile)
    Logger.info(s"Loaded Google credentials. Service account ID: ${credential.getServiceAccountId}")
    val impersonatedUser = ""
    new GoogleServiceAccount(credential.getServiceAccountId, credential.getServiceAccountPrivateKey, impersonatedUser)
  }
}
