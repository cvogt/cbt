package cbt
package mixins
import scala.collection.immutable.Seq
import java.io._
trait Test extends Build{
  lazy val testedBuild = BuildDependency( projectDirectory.parent )
  override def dependencies = Seq( testedBuild ) ++ super.dependencies
  override def scalaVersion = testedBuild.build.scalaVersion
}
trait Sbt extends Build{
  override def sources = Seq( projectDirectory ++ "/src/main/scala" )
}
trait SbtTest extends Test{
  override def sources = Vector( projectDirectory.parent ++ "/src/test/scala" )
}
trait ScalaTest extends Build with Test{
  def scalaTestVersion: String

  override def dependencies = super.dependencies :+ MavenRepository.central.resolve(
    "org.scalatest" %% "scalatest" % scalaTestVersion
  )

  override def run: ExitCode = {
    val discoveryPath = compile.toString++"/"
    context.logger.lib("discoveryPath: " ++ discoveryPath)
    lib.runMain(
      "org.scalatest.tools.Runner",
      Seq("-R", discoveryPath, "-oF") ++ context.args.drop(1),
      classLoader(context.classLoaderCache)
    )
  }
}
