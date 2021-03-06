package utils.auth.deadbolt

import javax.inject.{Inject, Singleton}

import be.objectify.deadbolt.scala.DeadboltActions
import com.mohiva.play.silhouette.api.Silhouette
import models.Roles
import play.api.mvc.{BodyParser, BodyParsers, Result, Results}
import utils.auth.DefaultEnv

import scala.concurrent.Future

/**
  * Provides actions that ensure authentication through Silhouette and authorization through Deadbolt
  *
  * @author Henrik Drefs
  */
@Singleton
class Auth @Inject()(deadboltActions: DeadboltActions, silhouette: Silhouette[DefaultEnv])
  extends Results with BodyParsers {

  def SubjectPresent[B](parser: BodyParser[B] = parse.anyContent)(block: SecureRequest[B] => Future[Result]) =
    silhouette.SecuredAction.async(parser) { implicit req =>
      deadboltActions.SubjectPresent()(parser)(block.compose(_ => new SecureRequest(req)))(req)
    }

  def Restrict[B](parser: BodyParser[B] = parse.anyContent)(role: Roles.Role)(block: SecureRequest[B] => Future[Result]) =
    silhouette.SecuredAction.async(parser) { implicit request =>
      deadboltActions.Restrict(role.group)(parser)(block.compose(_ => new SecureRequest(request)))(request)
    }

}
