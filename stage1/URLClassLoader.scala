package cbt

import java.net._
import scala.util.Try

case class URLClassLoader( classPath: ClassPath, parent: ClassLoader )( implicit val logger: Logger )
  extends java.net.URLClassLoader(
    classPath.strings.map( p => new URL("file:" ++ p) ).toArray,
    parent
  ) with CachingClassLoader{
  val id = Math.abs( new java.util.Random().nextInt )
  override def toString = (
    scala.Console.BLUE
      ++ getClass.getSimpleName ++ ":" ++ id.toString
      ++ scala.Console.RESET
      ++ "(\n"
      ++ (
        getURLs.map(_.toString).sorted.mkString(",\n")
        ++ (
          if(getParent() != ClassLoader.getSystemClassLoader().getParent())
            ",\n" ++ Option(getParent()).map(_.toString).getOrElse("null")
          else ""
        )
      ).split("\n").map("  "++_).mkString("\n")
      ++ "\n)"
  )
}

/*
trait ClassLoaderLogging extends ClassLoader{
  def logger: Logger
  val prefix = s"[${getClass.getSimpleName}] "
  val postfix = " in \name" ++ this.toString
  override def loadClass(name: String, resolve: Boolean): Class[_] = {
    //logger.resolver(prefix ++ s"loadClass($name, $resolve)" ++ postfix )
    super.loadClass(name, resolve)
  }
  override def loadClass(name: String): Class[_] = {
    //logger.resolver(prefix ++ s"loadClass($name)" ++ postfix )
    super.loadClass(name)
  }
  override def findClass(name: String): Class[_] = {
    //logger.resolver(prefix ++ s"findClass($name)" ++ postfix )
    super.findClass(name)
  }
}
*/
