package controllers

import javax.inject.Inject

import be.objectify.deadbolt.scala.{ActionBuilders, AuthenticatedRequest, DeadboltActions}
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import com.mohiva.play.silhouette.api.{LogoutEvent, Silhouette}
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import utils.auth.DefaultEnv

import scala.concurrent.Future

/**
  * The basic application controller.
  *
  * @param messagesApi            The Play messages API.
  * @param silhouette             The Silhouette stack.
  * @param socialProviderRegistry The social provider registry.
  * @param webJarAssets           The webjar assets implementation.
  */
class ApplicationController @Inject()(
                                       val messagesApi: MessagesApi,
                                       silhouette: Silhouette[DefaultEnv],
                                       deadboltActions: DeadboltActions,
                                       actionBuilders: ActionBuilders,
                                       socialProviderRegistry: SocialProviderRegistry,
                                       implicit val webJarAssets: WebJarAssets)
  extends Controller with I18nSupport {

  /**
    * Handles the index action.
    *
    * @return The result to display.
    */
  def index = silhouette.SecuredAction.async { implicit request =>
    implicit val authRequest = new AuthenticatedRequest(request, Some(request.identity))
    Future.successful(Ok(views.html.home(request.identity)))
  }

  type SilhouetteReq[B] = SecuredRequest[DefaultEnv, B]

  class SecureRequest[B](request: SilhouetteReq[B]) extends AuthenticatedRequest[B](request, Some(request.identity)) {
    val identity = request.identity
  }

  def SubjectPresent[B](block: SecureRequest[B] => Future[Result]) = silhouette.SecuredAction andThen new ActionTransformer[SilhouetteReq, SecureRequest] {
    override protected def transform[A](request: SilhouetteReq[A]) = Future.successful {
      new SecureRequest(request)
    }
  } andThen new ActionFunction[AuthenticatedRequest[B], AuthenticatedRequest[B]] {
    override def invokeBlock[A](request: AuthenticatedRequest[A], block: (AuthenticatedRequest[A]) => Future[Result]): Future[Result] = {
      deadboltActions.SubjectPresent()()(block)(request)
    }
  }

  def newIndex = SubjectPresent { implicit req =>
    Future.successful(Ok(views.html.home(req.identity)))
  }

  //  def i = deadboltActions.SubjectPresent()() { implicit request =>
  //    Future.successful(Ok(views.html.home(request.identity)))
  //  }

  /**
    * Handles the Sign Out action.
    *
    * @return The result to display.
    */
  def signOut = silhouette.SecuredAction.async { implicit request =>
    val result = Redirect(routes.ApplicationController.index())
    silhouette.env.eventBus.publish(LogoutEvent(request.identity, request))
    silhouette.env.authenticatorService.discard(request.authenticator, result)
  }
}
