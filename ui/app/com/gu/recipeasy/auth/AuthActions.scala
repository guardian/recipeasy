package com.gu.recipeasy.auth

import com.gu.googleauth._
import play.api.Configuration
import controllers.routes

trait AuthActions extends Actions {
  def conf: Configuration

  override val authConfig = GoogleAuthConfig(
    clientId = conf.getString("google.clientId").getOrElse(sys.error(s"Missing config key: google.clientId")),
    clientSecret = conf.getString("google.clientSecret").getOrElse(sys.error(s"Missing config key: google.clientSecret")),
    redirectUrl = conf.getString("google.redirectUrl").getOrElse(sys.error(s"Missing config key: google.redirectUrl")),
    domain = Some("guardian.co.uk")
  )
  // your app's routing
  override val loginTarget = routes.Login.loginAction
  override val defaultRedirectTarget = routes.Application.index
  override val failureRedirectTarget = routes.Login.login

}
