package cbt
import java.net._
import scala.collection.JavaConverters._

// do not make this a case class, required object identity equality
class MultiClassLoader(final val parents: Seq[ClassLoader])(implicit val logger: Logger) extends ClassLoader(null) with CachingClassLoader{
  override def findClass(name: String) = {
    parents.find( parent =>
      try{
        null != parent.loadClass(name) // FIXME: is it correct to just ignore the resolve argument here?
      } catch {
        case _:ClassNotFoundException => false
      }
    ).map(
      _.loadClass(name)
    ).getOrElse( null )
  }

  // FIXME: is there more than findClass and findResource that needs to be dispatched?
  override def findResource(name: String): URL = {
    parents.foldLeft(null: URL)(
      (acc, parent) => if( acc == null ) parent.getResource(name) else null
    )
  }
  override def findResources(name: String): java.util.Enumeration[URL] = {
    java.util.Collections.enumeration(
      parents.flatMap( _.getResources(name).asScala ).asJava
    )
  }

  override def toString = (
    scala.Console.BLUE
      ++ super.toString
      ++ scala.Console.RESET
      ++ "("
      ++ (
        if(parents.nonEmpty)(
          "\n" ++ parents.map(_.toString).mkString(",\n").split("\n").map("  "++_).mkString("\n") ++ "\n"
        ) else ""
      ) ++")"
  )
}
