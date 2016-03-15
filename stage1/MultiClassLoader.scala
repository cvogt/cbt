package cbt
import java.net._
import scala.util.Try
import scala.collection.immutable.Seq

class MultiClassLoader(parents: Seq[ClassLoader])(implicit val logger: Logger) extends ClassLoader with CachingClassLoader{
  override def findClass(name: String) = {
    parents.find( parent =>
      try{
        parent.loadClass(name)
        true
      } catch {
        case _:ClassNotFoundException => false
      }
    ).map(
      _.loadClass(name)
    ).getOrElse( throw new ClassNotFoundException(name) )
  }
  override def toString = (
    scala.Console.BLUE
      ++ super.toString
      ++ scala.Console.RESET
      ++ "("
      ++ (
        if(parents.nonEmpty)(
          "\n" ++ parents.map(_.toString).sorted.mkString(",\n").split("\n").map("  "++_).mkString("\n") ++ "\n"
        ) else ""
      ) ++")"
  )
}
