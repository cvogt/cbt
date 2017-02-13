package cbt.example.resources
import scala.collection.JavaConverters._
import java.nio.file.{Files, Paths}
object Main{
  def getResource = Option(getClass.getClassLoader.getResource("foo.text"))
  def getResources = getClass.getClassLoader.getResources("foo.text").asScala.toList

  def main( args: Array[String] ): Unit = {
    println("Reading parent's resources")
    println("")
    // resources are in the parent
    println("via child: " + getResource.nonEmpty + " " + getResources.size )
    println("")
    // this one is where the resources are
    println("via parent: " + parent.getResource.nonEmpty + " " + parent.getResources.size )
    println("")
    // parent is parent.parent's child. reading children's resources only works
    // with a flat classloader in cbt. Try `override def flatClassLoader = true`.
    // or `cbt runFlat` when extending `DynamicOverrides`
    println("via parent.parent: " + parent.parent.getResource.nonEmpty + " " + parent.parent.getResources.size )
  }
}
