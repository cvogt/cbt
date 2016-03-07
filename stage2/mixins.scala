package cbt
package mixins
import scala.collection.immutable.Seq
import java.io._
trait Test extends Build{
  lazy val testedBuild = BuildDependency(projectDirectory+"/../")
  override def dependencies = Seq( testedBuild ) ++ super.dependencies
  override def scalaVersion = testedBuild.build.scalaVersion
}
trait Sbt extends Build{
  override def sources = Seq(new File(projectDirectory+"/src/main/scala/"))
}
trait SbtTest extends Test{
  override def sources = Vector(new File(projectDirectory+"/../src/test/scala"))
}
trait ScalaTest extends Build with Test{
  def scalaTestVersion: String

  override def dependencies = Seq(
    "org.scalatest" %% "scalatest" % scalaTestVersion
  ) ++ super.dependencies

  // workaround probable ScalaTest bug throwing away the outer classloader. Not caching doesn't nest them.
  override def cacheDependencyClassLoader = false

  override def run: ExitCode = {
    val discoveryPath = compile.toString+"/"
    context.logger.lib("discoveryPath: "+discoveryPath)
    lib.runMain(
      "org.scalatest.tools.Runner",
      Seq("-R", discoveryPath, "-oF") ++ context.args.drop(1),
      classLoader
    )
  }
}
