package com.gu.typerighter.lib

import com.gu.pandomainauth.model.{Authenticated, AuthenticatedUser, AuthenticationStatus, User}
import com.gu.pandomainauth.{PanDomain, PublicKey, PublicSettings}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class UserRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request)

trait PandaAuthentication extends BaseControllerHelpers with Loggable {
  def publicSettings: PublicSettings

  def unauthorisedResponse[A](request: Request[A]) = {
    Future.successful(Unauthorized("Unauthorised"))
  }

  def authStatus(cookie: Cookie, publicKey: PublicKey): AuthenticationStatus = {
    PanDomain.authStatus(
      cookie.value,
      publicKey,
      PanDomain.guardianValidation,
      apiGracePeriod = 2 * 60 * 60 * 10000, // two hours worth of milliseconds
      system = "typerighter",
      cacheValidation = false
    )
  }

  object ApiAuthAction extends ActionBuilder[UserRequest, AnyContent] {
    override def parser: BodyParser[AnyContent] = PandaAuthentication.this.controllerComponents.parsers.default
    override protected def executionContext: ExecutionContext = PandaAuthentication.this.controllerComponents.executionContext

    override def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] = {
      publicSettings.publicKey match {
        case Some(pk) =>
          request.cookies.get("gutoolsAuth-assym") match {
            case Some(cookie) =>
              authStatus(cookie, pk) match {
                case Authenticated(AuthenticatedUser(user, _, _, _, _)) =>
                  block(new UserRequest(user, request))

                case other =>
                  log.info(s"Login response $other")
                  unauthorisedResponse(request)
              }

            case None =>
              log.warn("Panda cookie missing")
              unauthorisedResponse(request)
          }

        case None =>
          log.error("Panda public key unavailable")
          unauthorisedResponse(request)
      }
    }
  }
}
