package cbt

import java.net._
import java.util.concurrent.ConcurrentHashMap

class ClassLoaderCache(logger: Logger){
  val permanent = new KeyLockedLazyCache(
    NailgunLauncher.classLoaderCache.asInstanceOf[ConcurrentHashMap[String,AnyRef]],
    NailgunLauncher.classLoaderCache.asInstanceOf[ConcurrentHashMap[AnyRef,ClassLoader]],
    logger
  )
  val transient = new KeyLockedLazyCache(
    new ConcurrentHashMap[String,AnyRef],
    new ConcurrentHashMap[AnyRef,ClassLoader],
    logger
  )
}

private[cbt] class LockableKey
/**
A cache that lazily computes values if needed during lookup.
Locking occurs on the key, so separate keys can be looked up
simultaneously without a deadlock.
*/
final private[cbt] class KeyLockedLazyCache[Key <: AnyRef,Value <: AnyRef](
  keys: ConcurrentHashMap[Key,AnyRef],
  builds: ConcurrentHashMap[AnyRef,Value],
  logger: Logger
){
  def get( key: Key, value: => Value ): Value = {
    val keyObject = keys.synchronized{    
      if( ! (keys containsKey key) ){
        logger.resolver("CACHE MISS: " ++ key.toString)
        keys.put( key, new LockableKey )
      } else {
        logger.resolver("CACHE HIT: " ++ key.toString)
      }
      keys get key
    }
    import collection.JavaConversions._
    logger.resolver("CACHE: \n" ++ keys.mkString("\n"))
    def k = ClassPath(new java.io.File("c")).asInstanceOf[Key]
    // synchronizing on key only, so asking for a particular key does
    // not block the whole cache, but just that cache entry
    key.synchronized{
      if( ! (builds containsKey keyObject) ){
        builds.put( keyObject, value )
      }
      builds get keyObject      
    }
  }
  def remove( key: Key ) = keys.synchronized{    
    if( (keys containsKey key) ){
      keys.put( key, new LockableKey )
    }
    val keyObject = keys get key
    keys.remove( key )
    builds.remove( keyObject )
  }
}
