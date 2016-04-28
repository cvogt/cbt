package cbt

import java.net._
import java.util.concurrent.ConcurrentHashMap
import collection.JavaConversions._

case class ClassLoaderCache(
  logger: Logger,
  private[cbt] permanentKeys: ConcurrentHashMap[String,AnyRef],
  private[cbt] permanentClassLoaders: ConcurrentHashMap[AnyRef,ClassLoader]
){
  val persistent = new KeyLockedLazyCache(
    permanentKeys,
    permanentClassLoaders,
    Some(logger)
  )
  override def toString = (
    s"ClassLoaderCache("
    ++
    persistent.keys.keySet.toVector.map(_.toString.split(":").mkString("\n")).sorted.mkString("\n\n","\n\n","\n\n")
    ++
    ")"
  )
}
