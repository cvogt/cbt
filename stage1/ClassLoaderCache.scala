package cbt

import java.net._

private[cbt] object ClassLoaderCache{
  private val cache = NailgunLauncher.classLoaderCache
  def get( classpath: ClassPath )(implicit logger: Logger): ClassLoader
    = cache.synchronized{
    val lib = new Stage1Lib(logger)
    val key = classpath.strings.sorted.mkString(":")
    if( cache.containsKey(key) ){
      logger.resolver("CACHE HIT: "++key)
      cache.get(key)
    } else {
      logger.resolver("CACHE MISS: "++key)
      val cl = new cbt.URLClassLoader( classpath, ClassLoader.getSystemClassLoader )
      cache.put( key, cl )
      cl
    }
  }
  def remove( classpath: ClassPath ) = {
    val key = classpath.strings.sorted.mkString(":")
    cache.remove( key )
  }
}
