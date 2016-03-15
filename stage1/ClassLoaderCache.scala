package cbt

import java.net._
import java.util.concurrent.ConcurrentHashMap

class ClassLoaderCache(logger: Logger){
  val persistent = new KeyLockedLazyCache(
    NailgunLauncher.classLoaderCache.asInstanceOf[ConcurrentHashMap[String,AnyRef]],
    NailgunLauncher.classLoaderCache.asInstanceOf[ConcurrentHashMap[AnyRef,ClassLoader]],
    Some(logger)
  )
  val transient = new KeyLockedLazyCache(
    new ConcurrentHashMap[String,AnyRef],
    new ConcurrentHashMap[AnyRef,ClassLoader],
    Some(logger)
  )
}
