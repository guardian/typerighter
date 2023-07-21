package com.gu.typerighter.controllers

import com.gu.pandahmac.HMACAuthActions
import com.gu.pandomainauth.{PanDomain, PanDomainAuthSettingsRefresher}
import com.gu.pandomainauth.action.AuthActions
import com.gu.pandomainauth.model.AuthenticatedUser
import com.gu.typerighter.lib.{CommonConfig, Loggable}
import play.api.libs.ws.WSClient
import play.api.mvc.{BaseController, ControllerComponents}

@scala.annotation.nowarn("msg=early initializers")
abstract class PandaAuthController(
    val controllerComponents: ControllerComponents,
    config: CommonConfig
) extends {
      // This approach will be replaced with trait parameters in Scala 3 –
      // until then, this is where we can break out config parameters into
      // the overrides that our auth traits depend on. If we don't do that,
      // the overrides will not be present when the auth traits initialise.
      val panDomainSettings: PanDomainAuthSettingsRefresher = config.panDomainSettings
      val wsClient: WSClient = config.ws
    }
    with AuthActions
    with HMACAuthActions
    with BaseController
    with Loggable {
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
    s"https://${config.serviceName}.typerighter.${config.stageDomain}/oauthCallback"

  override def secretKeys =
    config.hmacSecrets
}
