/*
package cbt
import java.net._
import scala.util.Try

import scala.collection.immutable.Seq


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
