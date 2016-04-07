package cbt

import java.net._
import java.util.concurrent.ConcurrentHashMap
import collection.JavaConversions._

class ClassLoaderCache(logger: Logger){
  val persistent = new KeyLockedLazyCache(
    NailgunLauncher.classLoaderCacheKeys.asInstanceOf[ConcurrentHashMap[String,AnyRef]],
    NailgunLauncher.classLoaderCacheValues.asInstanceOf[ConcurrentHashMap[AnyRef,ClassLoader]],
    Some(logger)
  )
  val transient = new KeyLockedLazyCache(
    new ConcurrentHashMap[String,AnyRef],
    new ConcurrentHashMap[AnyRef,ClassLoader],
    Some(logger)
  )
  override def toString = (
    s"ClassLoaderCache("
    ++
    persistent.keys.keySet.toVector.map(_.toString.split(":").mkString("\n")).sorted.mkString("\n\n","\n\n","\n\n")
    ++
    "---------"
    ++
    transient.keys.keySet.toVector.map(_.toString.split(":").mkString("\n")).sorted.mkString("\n\n","\n\n^","\n\n")
    ++
    ")"
  )
}
