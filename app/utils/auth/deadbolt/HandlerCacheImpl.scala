package utils.auth.deadbolt

import javax.inject.{ Inject, Singleton }

import be.objectify.deadbolt.scala.{ DeadboltHandler, HandlerKey }
import be.objectify.deadbolt.scala.cache.HandlerCache

/**
 * Deadbolt's handler cache implementation, this gets bound as HandlerCache through the DeadboltHookImpl
 *
 * @author Henrik Drefs
 */
@Singleton
class HandlerCacheImpl @Inject() (defaultHandler: DeadboltHandlerImpl) extends HandlerCache {

  override def apply(): DeadboltHandler = defaultHandler

  override def apply(v1: HandlerKey): DeadboltHandler = defaultHandler
}
