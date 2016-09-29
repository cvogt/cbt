package cbt
import java.io._
import java.nio.file._
import java.net._
trait Scaffold{
  def logger: Logger

  private def createFile( projectDirectory: File, fileName: String, code: String ){
    val outputFile = projectDirectory ++ ("/" ++ fileName)
    Stage0Lib.write( outputFile, code, StandardOpenOption.CREATE_NEW )
    import scala.Console._
    println( GREEN ++ "Created " ++ fileName ++ RESET )
  }

  def createMain(
    projectDirectory: File
  ): Unit = { 
    createFile(projectDirectory, "Main.scala", s"""object Main{
  def main( args: Array[String] ): Unit = {
    println( Console.GREEN ++ "Hello World" ++ Console.RESET )
  }
}
"""
    )
  }

  def createBuild(
    projectDirectory: File
  ): Unit = {
    createFile(projectDirectory, "build/build.scala", s"""import cbt._
class Build(val context: Context) extends BaseBuild{
  override def dependencies =
    super.dependencies ++ // don't forget super.dependencies here for scala-library, etc.
    Seq(
      // source dependency
      // DirectoryDependency( projectDirectory ++ "/subProject" )
    ) ++
    // pick resolvers explicitly for individual dependencies (and their transitive dependencies)
    Resolver( mavenCentral, sonatypeReleases ).bind(
      // CBT-style Scala dependencies
      // ScalaDependency( "com.lihaoyi", "ammonite-ops", "0.5.5" )
      // MavenDependency( "com.lihaoyi", "ammonite-ops_2.11", "0.5.5" )

      // SBT-style dependencies
      // "com.lihaoyi" %% "ammonite-ops" % "0.5.5"
      // "com.lihaoyi" % "ammonite-ops_2.11" % "0.5.5"
    )
}
"""
    )
  }
}
