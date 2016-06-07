package cbt
package mixins
import java.net.URL
import java.io._
trait Test extends BaseBuild{
  lazy val testedBuild = BuildDependency( projectDirectory.parent )
  override def dependencies = Seq( testedBuild ) ++ super.dependencies
  override def defaultScalaVersion = testedBuild.build.scalaVersion
}
trait SbtTest extends Test{
  override def sources = Vector( projectDirectory.parent ++ "/src/test/scala" )
}

trait ScalaParadise extends BaseBuild{
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

trait Suggested extends BaseBuild{
  override def scalacOptions = super.scalacOptions ++ Seq(
    "-language:experimental.macros"
  )
}

trait Github extends Publish{
  def user: String
  def githubProject = name
  def githubUser = user
  final def githubUserProject = githubUser ++ "/" ++ githubProject
  override def url = new URL(s"http://github.com/$githubUserProject")
  override def scmUrl = s"git@github.com:$githubUserProject.git"
  override def scmConnection = s"scm:git:$scmUrl"
}
