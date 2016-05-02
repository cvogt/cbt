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

  def createBasicBuild(
    projectDirectory: File
  ): Unit = { 
    createFile(projectDirectory, "build/build.scala", s"""import cbt._
import java.net.URL
import java.io.File
import scala.collection.immutable.Seq

class Build(context: Context) extends BasicBuild(context){
  override def dependencies = {  // don't forget super.dependencies here
    super.dependencies ++ Resolver( mavenCentral ).bind(
      /*
      // Scala dependency automatically adding Scala major version to artifact id
      ScalaDependency("com.lihaoyi", "ammonite-ops", "0.5.5")
      // SBT-style Scala dependency
      "com.lihaoyi" %% "ammonite-ops" % "0.5.5"
      // Java dependency directly corresponding to maven artifact id
      MavenDependency("com.lihaoyi", "ammonite-ops_2.11", "0.5.5")
      // SBT-style Java dependency
      "com.lihaoyi" % "ammonite-ops_2.11" % "0.5.5"
      */
    )
  }
}
"""
    )

  }

  def createBuildBuild(
    projectDirectory: File
  ): Unit = { 
    createFile(projectDirectory, "build/build/build.scala", s"""import cbt._
import java.net.URL
import java.io.File
import scala.collection.immutable.Seq

class Build(context: Context) extends BuildBuild(context){
  override def dependencies = {  // don't forget super.dependencies here
    super.dependencies ++ Resolver( mavenCentral ).bind(
      /*
      // Scala dependency automatically adding Scala major version to artifact id
      ScalaDependency("com.lihaoyi", "ammonite-ops", "0.5.5")
      // SBT-style Scala dependency
      "com.lihaoyi" %% "ammonite-ops" % "0.5.5"
      // Java dependency directly corresponding to maven artifact id
      MavenDependency("com.lihaoyi", "ammonite-ops_2.11", "0.5.5")
      // SBT-style Java dependency
      "com.lihaoyi" % "ammonite-ops_2.11" % "0.5.5"
      */
    )
  }
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