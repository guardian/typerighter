package services

import com.gu.pandomainauth.PanDomain
import com.gu.pandomainauth.model.AuthenticationStatus
import com.gu.pandomainauth.service.CryptoConf.Verification

/** A thin layer over pan-domain-authentication to facilitate macking for tests.
  */
class LambdaAuth {
  def authenticateCookie(
      cookie: String,
      appName: String,
      verification: Verification
  ): AuthenticationStatus =
    PanDomain.authStatus(
      cookieData = cookie,
      verification = verification,
      validateUser = PanDomain.guardianValidation,
      apiGracePeriod = 1000 * 60 * 60,
      system = appName,
      cacheValidation = true,
      forceExpiry = true
    )
}
