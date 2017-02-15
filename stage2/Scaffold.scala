package cbt
import java.io._
import java.nio.file._
import java.net._
class Scaffold( logger: Logger ){
  val lib = new Lib(logger)

  private def createFile( projectDirectory: File, fileName: String, code: String ){
    val outputFile = projectDirectory ++ ("/" ++ fileName)
    Stage0Lib.write( outputFile, code, StandardOpenOption.CREATE_NEW )
    import scala.Console._
    println( GREEN ++ "Created " ++ fileName ++ RESET )
  }

  private[cbt] def packageName(name: String) = {
    def stripNonAlPrefix = (_:String).dropWhile(
      !(('a' to 'z') ++ ('A' to 'Z') ++ Seq('_')).contains(_)
    )
    def removeNonAlNumPlusSelected = "([^-a-zA-Z0-9_\\.\\\\/])".r.replaceAllIn(_:String, "")
    def replaceSpecialWithUnderscore = "([-\\. ])".r.replaceAllIn(_:String, "_")
    def removeRepeatedDots = "\\.+".r.replaceAllIn(_:String, ".")
    val transform = (
      (
        stripNonAlPrefix
        andThen
        removeNonAlNumPlusSelected
        andThen
        replaceSpecialWithUnderscore
      ).andThen(
        (_:String).replace("/",".").replace("\\",".").toLowerCase
      ) andThen removeRepeatedDots

    )

    transform( name )
  }

  private[cbt] def packageFromDirectory(directory: File) = {
    packageName(
      directory.getAbsolutePath.stripPrefix(
        lib.findOuterMostModuleDirectory( directory ).getParentFile.getAbsolutePath
      )
    )
  }

  def createMain(
    projectDirectory: File
  ): Unit = {
    createFile(projectDirectory, "Main.scala", s"""package ${packageFromDirectory(projectDirectory)}
object Main{
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
    createFile(projectDirectory, lib.buildDirectoryName++"/"++lib.buildFileName, s"""package cbt_build.${packageFromDirectory(projectDirectory)}
import cbt._
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
object ScaffoldTest{
  val scaffold = new Scaffold(new Logger(None,System.currentTimeMillis))
  import scaffold._
  def main(args: Array[String]): Unit = {
    def assertEquals[T](left: T, right: T) = {
      assert( left == right, left + " == " + right )
    }
    assertEquals(
      packageName( "AsdfAsdfAsdf" ), "asdfasdfasdf"
    )
    assertEquals(
      packageName( "_AsdfA4sdf" ), "_asdfa4sdf"
    )
    assertEquals(
      packageName( "-AsdfAsdf" ), "asdfasdf"
    )
    assertEquals(
      packageName( "asdf 4aSdf" ), "asdf4asdf"
    )
    assertEquals(
      packageName( "&/(&%$&&/(asdf" ), "asdf"
    )
    assertEquals(
      packageName( "AAA" ), "aaa"
    )
    assertEquals(
      packageName( "/AAA/a_a/a.a" ), "aaa.a_a.a_a"
    )
  }
}
