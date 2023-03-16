package com.gu.typerighter.lib

import com.gu.pandomainauth.model.{
  Authenticated,
  AuthenticatedUser,
  AuthenticationStatus,
  Expired,
  GracePeriod,
  User
}
import com.gu.pandomainauth.{PanDomain, PublicKey, PublicSettings}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import akka.stream.scaladsl.Flow

class UserRequest[A](val user: User, request: Request[A]) extends WrappedRequest[A](request)

trait PandaAuthentication extends BaseControllerHelpers with Loggable {
  def publicSettings: PublicSettings

  val unauthorisedResponse = Future.successful(Left(Unauthorized("Unauthorised")))

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
    override def parser: BodyParser[AnyContent] =
      PandaAuthentication.this.controllerComponents.parsers.default
    override protected def executionContext: ExecutionContext =
      PandaAuthentication.this.controllerComponents.executionContext

    override def invokeBlock[A](
        request: Request[A],
        block: UserRequest[A] => Future[Result]
    ): Future[Result] =
      invoke(request) { (user, request) =>
        val userRequest = new UserRequest(user, request)
        block(userRequest)
      }.map(_.fold(identity, identity))(executionContext)

    def invoke[Req <: RequestHeader, A, T](
        request: Req
    )(block: (User, Req) => Future[T]): Future[Either[Result, T]] =
      (publicSettings.publicKey, request.cookies.get("gutoolsAuth-assym")) match {
        case (Some(pk), Some(cookie)) =>
          authStatus(cookie, pk) match {
            case Authenticated(AuthenticatedUser(user, _, _, _, _)) =>
              block(user, request).map(Right(_))(executionContext)
            case GracePeriod(AuthenticatedUser(user, _, _, _, _)) =>
              log.info(
                s"User ${user.email} has made a request with a token within the auth grace period"
              )
              block(user, request).map(Right(_))(executionContext)
            case Expired(AuthenticatedUser(user, _, _, _, _)) =>
              log.info(s"User ${user.email} has made a request with an expired token")
              block(user, request).map(Right(_))(executionContext)
            case other =>
              log.info(s"Login response $other")
              unauthorisedResponse
          }
        case (None, _) =>
          log.error("Panda public key unavailable")
          unauthorisedResponse
        case (_, None) =>
          log.warn("Panda cookie missing")
          unauthorisedResponse
      }

    def toSocket[A, In, Out](request: RequestHeader)(
        f: (User, RequestHeader) => Flow[In, Out, _]
    ): Future[Either[Result, Flow[In, Out, _]]] =
      invoke(request)((u, r) => Future.successful(f(u, r)))
  }
}
