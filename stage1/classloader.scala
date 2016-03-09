package cbt
import java.io._
import java.net._
import java.nio.file._
import scala.util.Try

import scala.collection.immutable.Seq

object ClassLoaderCache{
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
/*
class MultiClassLoader(parents: Seq[ClassLoader]) extends ClassLoader {
  override def loadClass(name: String) = {
    //System.err.println("LOADING CLASS "++name);
    val c = parents.toStream.map{
      parent =>
      Try{
        parent.loadClass(name)
      }.map(Option[Class[_]](_)).recover{
        case _:ClassNotFoundException => None
      }.get
    }.find(_.isDefined).flatten
    c.getOrElse( ClassLoader.getSystemClassLoader.loadClass(name) )
  }
  override def toString = "MultiClassLoader(" ++ parents.mkString(",") ++ ")"
}
*/
case class URLClassLoader(classPath: ClassPath, parent: ClassLoader)
  extends java.net.URLClassLoader(
    classPath.strings.map(
      path => new URL("file:"++path)
    ).toArray,
    parent
  ){
  override def toString = (
    scala.Console.BLUE ++ "cbt.URLClassLoader" ++ scala.Console.RESET
      ++ "(\n  " ++ getURLs.map(_.toString).sorted.mkString(",\n  ")
      ++ (
        if(getParent() != ClassLoader.getSystemClassLoader())
          ",\n" ++ getParent().toString.split("\n").map("  "++_).mkString("\n")
        else ""
      )
      ++ "\n)"
  )
}
