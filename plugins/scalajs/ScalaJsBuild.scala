package cbt
import java.io.File
import java.net.URL

trait ScalaJsBuild extends DynamicOverrides{
  final protected val scalaJsLib = ScalaJsLib(
    scalaJsVersion, scalaVersion, context.cbtLastModified, context.paths.mavenCache
  )
  import scalaJsLib.{link => _,_}

  def scalaJsVersion = "0.6.8"
  final protected val scalaJsMajorVersion: String = lib.libMajorVersion(scalaJsVersion)
  final protected val artifactIdSuffix = s"_sjs$scalaJsMajorVersion"

  override def dependencies = super.dependencies :+ scalaJsLibraryDependency
  override def scalacOptions = super.scalacOptions ++ scalaJsLib.scalacOptions

  /** Note: We make same assumption about scala version.
      In order to be able to choose different scala version, one has to use %. */
  implicit class ScalaJsDependencyBuilder(groupId: String){
    def %%%(artifactId: String) = new DependencyBuilder2(
      groupId, artifactId + artifactIdSuffix, Some(scalaMajorVersion))
  }

  override def compile = {
    val res = super.compile
    scalaJsLib.link( scalaJsTargetFile, scalaJsOptions, target +: dependencyClasspath.files )
    res
    // FIXME: we need to rethink the concept of a "compile" task I think.
    // An exit code would probably be more appropriate here.
  }

  def scalaJsOptions: Seq[String] = Seq()

  /** Where to put the generated js file */
  def scalaJsTargetFile: File = target / "app.js"

  override def cleanFiles = super.cleanFiles :+ scalaJsTargetFile :+ (scalaJsTargetFile ++ ".map")

  def fullOpt = newBuild[ScalaJsBuild]("""
    override def scalaJsOptions = "--fullOpt" +: super.scalaJsOptions
  """)
}
