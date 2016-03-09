package cbt
import java.io._
import java.net._
import ammonite.ops.{cwd => _,_}

trait Scaffold{
  def logger: Logger
  
  def generateBasicBuildFile(
    projectDirectory: File,
    scalaVersion: String,
    groupId: String,
    artifactId: String,
    version: String
  ): Unit = { 
  /**
  TODO:
   - make behavior more user friendly:
     - not generate half and then throw exception for one thing already existing
     - maybe not generate all of this, e.g. offer different variants
  */

    val generatedFiles = Seq(
      "build/build.scala" -> s"""import cbt._
import java.net.URL
import java.io.File
import scala.collection.immutable.Seq

class Build(context: Context) extends BasicBuild(context) with BuildShared{
  override def artifactId: String = "$artifactId"
  override def groupId = "$groupId"

  override def dependencies = super.dependencies ++ Seq(  // don't forget super.dependencies here
    // "org.cvogt" %% "scala-extensions" % "0.4.1"
  )

  // required for .pom file
  override def name = artifactId
  override def description       : String = lib.requiredForPom("description")
}
""",

      "build/build/build.scala" -> s"""import cbt._
import java.net.URL
import java.io.File
import scala.collection.immutable.Seq

class Build(context: Context) extends BuildBuild(context){
  override def scalaVersion: String = "2.11.8"
  
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
"""
    )

    generatedFiles.map{
      case ( fileName, code ) =>
        scala.util.Try{
          write( Path( projectDirectory.string ++ "/" ++ fileName ), code )
          import scala.Console._
          println( GREEN ++ "Created " ++ fileName ++ RESET )
        }
    }.foreach(
      _.recover{
        case e: java.nio.file.FileAlreadyExistsException =>
          e.printStackTrace
      }.get
    )
    return ()
  }

}