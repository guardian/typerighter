package controllers

import com.gu.pandomainauth.PanDomain
import com.gu.pandomainauth.action.AuthActions
import com.gu.pandomainauth.model.AuthenticatedUser
import com.gu.typerighter.lib.Loggable
import org.slf4j.LoggerFactory
import play.api.{Configuration}
import com.gu.typerighter.lib.CommonConfig

trait AppAuthActions extends AuthActions with Loggable {
  def config: CommonConfig

  override def validateUser(authedUser: AuthenticatedUser): Boolean = {
    log.info(s"Validating user $authedUser")
    PanDomain.guardianValidation(authedUser)
  }

  /**
    * By default, the user validation method is called every request. If your validation
    * method has side-effects or is expensive (perhaps hitting a database), setting this
    * to true will ensure that validateUser is only called when the OAuth session is refreshed
    */
  override def cacheValidation = false

  override def authCallbackUrl: String = s"https://typerighter.${config.stageDomain}/oauthCallback"
}
