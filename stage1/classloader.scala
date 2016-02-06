package cbt
import java.io._
import java.net._
import java.nio.file._
import scala.util.Try

import scala.collection.immutable.Seq

object ClassLoaderCache{
  private val cache = NailgunLauncher.classLoaderCache
  def classLoader( path: String, parent: ClassLoader ): ClassLoader = {
    def realpath( name: String ) = Paths.get(new File(name).getAbsolutePath).normalize.toString
    val normalized = realpath(path)
    if( cache.containsKey(normalized) ){
      //println("FOUND: "+normalized)
      cache.get(normalized)
    } else {
      //println("PUTTING: "+normalized)
      //Try(???).recover{ case e=>e.printStackTrace}
      val cl = new cbt.URLClassLoader( ClassPath(Seq(new File(normalized))), parent )
      cache.put( normalized, cl )
      cl
    }
  }
  def remove( path: String ) = cache.remove( path )
}
class MultiClassLoader(parents: Seq[ClassLoader]) extends ClassLoader {
  override def loadClass(name: String) = {
    //System.err.println("LOADING CLASS "+name);
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
  override def toString = "MultiClassLoader(" + parents.mkString(",") + ")"
}
case class URLClassLoader(classPath: ClassPath, parent: ClassLoader)
  extends java.net.URLClassLoader(
    classPath.strings.map(
      path => new URL("file:"+path)
    ).toArray,
    parent
  ){
  override def toString = (
    scala.Console.BLUE + "cbt.URLClassLoader" + scala.Console.RESET +  "(\n  " + getURLs.map(_.toString).sorted.mkString(",\n  ")
    + (if(getParent() != ClassLoader.getSystemClassLoader()) ",\n" + getParent().toString.split("\n").map("  "+_).mkString("\n") else "")
    + "\n)"   
  )
  import scala.language.existentials
  /*override def loadClass(name: String): Class[_] = {
    //System.err.println("LOADING CLASS "+name+" in "+this);
    try{
      super.loadClass(name)
    } catch {
      case e: ClassNotFoundException =>
        // FIXME: Shouldn't this happen automatically?
        parent.loadClass(name)
    }
  }*/
}
