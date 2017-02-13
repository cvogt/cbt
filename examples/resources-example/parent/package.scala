package cbt.example.resources.parent
import scala.collection.JavaConverters._
object `package`{
  def getResource = Option(getClass.getClassLoader.getResource("foo.text"))
  def getResources = getClass.getClassLoader.getResources("foo.text").asScala.toList
}
