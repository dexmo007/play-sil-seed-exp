package utils.auth.deadbolt

import be.objectify.deadbolt.scala.cache.HandlerCache
import play.api.{ Configuration, Environment }
import play.api.inject.{ Binding, Module }

/**
 * The Deadbolt module that is hooked by play config
 *
 * @author Henrik Drefs
 */
class DeadboltHookImpl extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = Seq(
    bind[HandlerCache].to[HandlerCacheImpl]
  )
}
