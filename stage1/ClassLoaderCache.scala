package cbt

import java.net._
import java.util._
import collection.JavaConverters._

case class ClassLoaderCache(
  logger: Logger,
  private[cbt] hashMap: java.util.Map[AnyRef,AnyRef]
){
  val cache = new KeyLockedLazyCache[ClassLoader]( hashMap, Some(logger) )
  override def toString = (
    s"ClassLoaderCache("
    ++
    hashMap.asScala.collect{
      case (key, value) if key.isInstanceOf[String] =>
        key.toString.split(":").mkString("\n") -> value
    }.toVector.sortBy(_._1).map{
      case (key, value) => key + " -> " + hashMap.get(value)
    }.mkString("\n\n","\n\n","\n\n")
    ++
    ")"
  )
}
