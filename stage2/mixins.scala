package cbt
package mixins
import scala.collection.immutable.Seq
import java.io._
trait Test extends BasicBuild{
  lazy val testedBuild = BuildDependency( projectDirectory.parent )
  override def dependencies = Seq( testedBuild ) ++ super.dependencies
  override def defaultScalaVersion = testedBuild.build.scalaVersion
}
trait SbtTest extends Test{
  override def sources = Vector( projectDirectory.parent ++ "/src/test/scala" )
}

trait ScalaParadise extends BasicBuild{
  def scalaParadiseVersion = "2.1.0"

  private def scalaParadiseDependency =
    Resolver( mavenCentral ).bindOne(
      "org.scalamacros" % ("paradise_" ++ scalaVersion) % scalaParadiseVersion
    )

  override def dependencies = (
    super.dependencies // don't forget super.dependencies here
    ++ (
      if(scalaVersion.startsWith("2.10."))
        Seq(scalaParadiseDependency)
      else
        Seq()
    )
  )

  override def scalacOptions = (
    super.scalacOptions
    ++ (
      if(scalaVersion.startsWith("2.10."))
        Seq("-Xplugin:"++scalaParadiseDependency.exportedClasspath.string)
      else
        Seq()
    )
  )
}
