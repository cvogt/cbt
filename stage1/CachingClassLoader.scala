package cbt
import java.net._
import java.util.concurrent.ConcurrentHashMap
import scala.util.Try

trait CachingClassLoader extends ClassLoader{
  def logger: Logger
  val cache = new KeyLockedLazyCache[String,Try[Class[_]]]( new ConcurrentHashMap, new ConcurrentHashMap, Some(logger) )
  override def loadClass(name: String, resolve: Boolean) = {
    cache.get( name, Try(super.loadClass(name, resolve)) ).get
  }
}
