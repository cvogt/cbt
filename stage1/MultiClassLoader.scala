package cbt
import java.net._
import scala.collection.immutable.Seq

// do not make this a case class, required object identity equality
class MultiClassLoader(parents: Seq[ClassLoader])(implicit val logger: Logger) extends ClassLoader with CachingClassLoader{
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
