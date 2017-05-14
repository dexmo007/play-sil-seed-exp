package utils.auth.deadbolt

import be.objectify.deadbolt.scala.AuthenticatedRequest
import com.mohiva.play.silhouette.api.actions.SecuredRequest
import utils.auth.DefaultEnv

/**
  * Request wrapping Silhouette's [[SecuredRequest]] and extending Deadbolt's [[AuthenticatedRequest]]
  *
  * @author Henrik Drefs
  */
class SecureRequest[B](request: SecuredRequest[DefaultEnv, B]) extends AuthenticatedRequest[B](request, Some(request.identity)) {
  val identity = request.identity
}
