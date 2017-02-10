package cbt.example.resources
import java.nio.file.{Files, Paths}
object Main extends App {
  // Be aware that CBT currently isolates classloaders of dependencies
  // your dependencies will not see the resources of your project
  // This means that e.g. spray will not see a application.conf in your project's
  // resources/ directory. See https://github.com/cvogt/cbt/issues/176
  println(
    "foo.text in resources contains: " ++
    new String(
      Files.readAllBytes(
        Paths.get( getClass.getClassLoader.getResource("foo.text").getFile )
      )
    )
  )
  import scala.collection.JavaConverters._
  println(
    "foo.text in resources and my-resources:\n" ++
    getClass.getClassLoader.getResources("foo.text").asScala.map(
      resource =>
      new String(
        Files.readAllBytes(
          Paths.get( resource.getFile )
        )
      )
    ).mkString
  )
  println(
    "via parent foo.text in resources is: " ++ scala.runtime.ScalaRunTime.stringOf( parent.getChildResource )
  )
  println(
    "via parent foo.text in resources and my-resources is: " ++ parent.getChildResources.mkString
  )
}
