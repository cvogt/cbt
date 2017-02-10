package cbt.example.resources.parent
import scala.collection.JavaConverters._
object `package`{
  def getChildResource = getClass.getClassLoader.getResource("foo.text")
  def getChildResources = getClass.getClassLoader.getResources("foo.text").asScala.toList
}
