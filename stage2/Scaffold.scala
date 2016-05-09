package cbt
import java.io._
import java.nio.file._
import java.net._

trait Scaffold{
  def logger: Logger

  private def createFile( projectDirectory: File, fileName: String, code: String ){
    val outputFile = projectDirectory ++ ("/" ++ fileName)
    outputFile.getParentFile.mkdirs
    Files.write( ( outputFile ).toPath, code.getBytes, StandardOpenOption.CREATE_NEW )
    import scala.Console._
    println( GREEN ++ "Created " ++ fileName ++ RESET )
  }

  def scaffoldBasicBuild(
    projectDirectory: File
  ): Unit = { 
    createFile(projectDirectory, "build/build.scala", s"""import cbt._
import java.net.URL
import java.io.File
import scala.collection.immutable.Seq

class Build(context: Context) extends BasicBuild(context){
  override def dependencies = {  // don't forget super.dependencies here
    super.dependencies :+ MavenResolver(context.cbtHasChanged,context.paths.mavenCache,MavenResolver.central).resolve(
      MavenDependency("net.incongru.watchservice","barbary-watchservice","1.0"),
      MavenDependency("org.eclipse.jgit", "org.eclipse.jgit", "4.2.0.201601211800-r"),
      MavenDependency("com.typesafe.zinc","zinc","0.3.9"),
      ScalaDependency("org.scala-lang.modules","scala-xml","1.0.5")
    )
  }
}
"""
    )

  }

  def scaffoldBuildBuild(
    projectDirectory: File
  ): Unit = { 
    createFile(projectDirectory, "build/build/build.scala", s"""import cbt._
import java.net.URL
import java.io.File
import scala.collection.immutable.Seq

class Build(context: Context) extends BuildBuild(context){
  override def dependencies = super.dependencies ++ Seq(
    // , "com.lihaoyi" %% "ammonite-ops" % "0.5.5"
  )
}
"""
    )

  }

  def scaffoldSjsBuild(projectDirectory: File): Unit = {
    createFile(projectDirectory, "build/build.scala", s"""import cbt._
import java.net.URL
import java.io.File
import scala.collection.immutable.Seq

class Build(context: Context) extends SjsBuild(context){
  override val projectName = "my-project"
  override def dependencies = super.dependencies ++ Seq(  // don't forget super.dependencies here
    MavenResolver(context.cbtHasChanged, context.paths.mavenCache, MavenResolver.central).resolve(
      "org.scala-js" %% "scalajs-dom_sjs0.6" % "0.9.0", // for example
      "com.github.japgolly.scalajs-react" %%% "core" % "0.10.4" // for another example
    )
  )
}
"""
    )
  }
/*,

      "build/build/build.scala" -> s"""import cbt._
import java.net.URL
import java.io.File
import scala.collection.immutable.Seq

class Build(context: Context) extends BuildBuild(context){
  override def dependencies = super.dependencies ++ Seq(
    BuildDependency( projectDirectory.parent ++ "/build-shared")
    // , "com.lihaoyi" %% "ammonite-ops" % "0.5.5"
  )
}
""",

      "test/Main.scala" -> s"""object Main{
  def main( args: Array[String] ) = {
    assert( false, "Go. Write some tests :)!" )
  }
}
""",

      "test/build/build.scala" -> s"""import cbt._
import java.net.URL
import java.io.File
import scala.collection.immutable.Seq

class Build(context: cbt.Context) extends BasicBuild(context) with BuildShared/* with cbt.mixins.ScalaTest*/{
  // def scalaTestVersion = "2.2.6"

  override def dependencies = super.dependencies ++ Seq(
    // , "org.scalacheck" %% "scalacheck" % "1.13.0"
  )
}
""",

      "test/build/build/build.scala" -> s"""import cbt._
import java.net.URL
import java.io.File
import scala.collection.immutable.Seq

class Build(context: Context) extends BuildBuild(context){
  override def scalaVersion: String = "2.11.8"

  override def dependencies = super.dependencies ++ Seq(
    BuildDependency( projectDirectory.parent.parent ++ "/build-shared")
    // , "com.lihaoyi" %% "ammonite-ops" % "0.5.5"
  )
}
""",

      "build-shared/build/build.scala" -> s"""import cbt._
import java.net.URL
import java.io.File
import scala.collection.immutable.Seq

class Build(context: Context) extends BasicBuild(context){
  override def scalaVersion: String = "$scalaVersion"

  override def dependencies = super.dependencies ++ Seq(  // don't forget super.dependencies here
    CbtDependency
    // , "org.cvogt" %% "scala-extensions" % "0.4.1"
  )
}
""",

      "build-shared/BuildShared.scala" -> s"""import cbt._
import java.net.URL
import java.io.File
import scala.collection.immutable.Seq

trait BuildShared extends BasicBuild{
  override def scalaVersion: String = "$scalaVersion"
  override def enableConcurrency = false // enable for speed, disable for debugging

  override def groupId = "$groupId"
  override def version = "$version"

  // required for .pom file
  override def url                  : URL = lib.requiredForPom("url")
  override def developers: Seq[Developer] = lib.requiredForPom("developers")
  override def licenses    : Seq[License] = lib.requiredForPom("licenses")
  override def scmUrl            : String = lib.requiredForPom("scmUrl")
  override def scmConnection: String = lib.requiredForPom("scmConnection")
  override def pomExtra: Seq[scala.xml.Node] = Seq()
}
"""*/

}