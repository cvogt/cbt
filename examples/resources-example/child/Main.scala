package cbt.example.resources.child_cached
import java.nio.file.{Files, Paths}
import scala.collection.JavaConverters._
import java.net.URL
object Main{
  def main( args: Array[String] ): Unit = {
    // Be aware that CBT currently isolates classloaders of dependencies
    // your dependencies will not see the resources of your project
    // This means that e.g. spray will not see a application.conf in your project's
    // resources/ directory. See https://github.com/cvogt/cbt/issues/176
    assert(
      getClass.getClassLoader.getResource("foo.text").isInstanceOf[URL]
    )
    assert(
      getClass.getClassLoader.getResources("foo.text").asScala.toList.head.isInstanceOf[URL]
    )
    println("Success")
  }
}