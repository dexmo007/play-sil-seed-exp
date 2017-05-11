package utils.auth.deadbolt

import javax.inject.{ Inject, Singleton }

import be.objectify.deadbolt.scala.models.Subject
import be.objectify.deadbolt.scala.{ AuthenticatedRequest, DeadboltHandler, DynamicResourceHandler }
import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.util.ExtractableRequest
import models.services.UserService
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.{ Request, Result, Results }
import utils.auth.DefaultEnv

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by henri on 5/11/2017.
 */
@Singleton
class DeadboltHandlerImpl @Inject() (val messagesApi: MessagesApi, silhouette: Silhouette[DefaultEnv], userService: UserService)
  extends DeadboltHandler with I18nSupport {

  override def beforeAuthCheck[A](request: Request[A]): Future[Option[Result]] = Future.successful(None)

  override def getSubject[A](request: AuthenticatedRequest[A]): Future[Option[Subject]] = {
    implicit val extractableRequest = new ExtractableRequest(request)
    silhouette.env.authenticatorService.retrieve.flatMap {
      case Some(authenticator) =>
        userService.retrieve(authenticator.loginInfo)
      case None => Future.successful(None)
    }
  }

  override def onAuthFailure[A](request: AuthenticatedRequest[A]): Future[Result] = {
    println("Deadbolt: onAuthFailure triggered!")
    Future.successful(Results.Unauthorized) // todo or does this need to accept?
  }

  override def getDynamicResourceHandler[A](request: Request[A]): Future[Option[DynamicResourceHandler]] =
    Future.successful(None)
}
