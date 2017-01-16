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
  def getServiceAccount(configuration: Configuration) = {
    val certFile = new FileInputStream("/etc/gu/recipeasy-service-account.json")
    val credential = GoogleCredential.fromStream(certFile)
    Logger.info(s"Loaded Google credentials. Service account ID: ${credential.getServiceAccountId}")
    val impersonatedUser = configuration.getString("google.impersonatedUser").getOrElse(sys.error("Missing key: google.impersonatedUser"))
    new GoogleServiceAccount(credential.getServiceAccountId, credential.getServiceAccountPrivateKey, impersonatedUser)
  }
  private val checker = new GoogleGroupChecker(getServiceAccount(conf))
  def isAdmin(email: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    checker.retrieveGroupsFor(email).map(set => set.contains("recipeeasy.admin@guardian.co.uk"))
  }
}

class GoogleGroupsAuthorisationDummy() extends Authorisation {
  def isAdmin(email: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    Future.successful(true)
  }
}
