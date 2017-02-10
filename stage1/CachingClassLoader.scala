package cbt
import java.net._
import java.util.concurrent.ConcurrentHashMap
import scala.util.Try

trait CachingClassLoader extends ClassLoader{
  def logger: Logger
  val cache = new KeyLockedLazyCache[Option[Class[_]]]( new ConcurrentHashMap[AnyRef,AnyRef], Some(logger) )
  override def loadClass(name: String, resolve: Boolean) = {
    cache.get( name, Try(super.loadClass(name, resolve)).toOption ).getOrElse(null)
  }
  override def loadClass(name: String) = {
    val _class = super.loadClass(name)
    if(_class == null) throw new ClassNotFoundException(name)
    else _class
  }
}
