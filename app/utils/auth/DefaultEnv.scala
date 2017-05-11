package utils.auth

import com.mohiva.play.silhouette.api.Env
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import models.HQUser

/**
 * The default env.
 */
trait DefaultEnv extends Env {
  type I = HQUser
  type A = CookieAuthenticator
}
