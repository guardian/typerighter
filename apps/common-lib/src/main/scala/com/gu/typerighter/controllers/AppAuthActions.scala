package com.gu.typerighter.controllers

import com.gu.pandahmac.HMACAuthActions
import com.gu.pandomainauth.{PanDomain, PanDomainAuthSettingsRefresher}
import com.gu.pandomainauth.action.AuthActions
import com.gu.pandomainauth.model.AuthenticatedUser
import com.gu.typerighter.lib.{CommonConfig, Loggable}
import play.api.libs.ws.WSClient

trait AppAuthActions extends AuthActions with HMACAuthActions with Loggable {
  def config: CommonConfig

  val panDomainSettings: PanDomainAuthSettingsRefresher = config.panDomainSettings
  val wsClient: WSClient = config.ws

  override def validateUser(authedUser: AuthenticatedUser): Boolean = {
    log.info(s"Validating user $authedUser")
    PanDomain.guardianValidation(authedUser)
  }

  /** By default, the user validation method is called every request. If your validation method has
    * side-effects or is expensive (perhaps hitting a database), setting this to true will ensure
    * that validateUser is only called when the OAuth session is refreshed
    */
  override def cacheValidation = false

  override def authCallbackUrl: String =
    s"https://manager.typerighter.${config.stageDomain}/oauthCallback"
}
