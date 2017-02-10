package cbt
import java.io.File
import java.net.URL

trait ScalaJsBuild extends BaseBuild {
  final protected val scalaJsLib = ScalaJsLib(
    scalaJsVersion,
    scalaVersion,
    context.cbtLastModified,
    context.classLoaderCache,
    context.paths.mavenCache
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
  
  private def link(mode: ScalaJsOutputMode, outputPath: File) = scalaJsLib.link(
    mode, outputPath, scalaJsOptions,
    target +: dependencies.collect{case d: BoundMavenDependency => d.jar}
  )

  def scalaJsOptions: Seq[String] = Seq()
  def scalaJsOptionsFastOpt: Seq[String] = scalaJsOptions
  def scalaJsOptionsFullOpt: Seq[String] = scalaJsOptions

  private def output(mode: ScalaJsOutputMode) = target ++ s"/$projectName-${mode.fileSuffix}.js"
  protected def fastOptJSFile: File = output(FastOptJS)
  protected def fullOptJSFile: File = output(FullOptJS)

  def fastOptJS = {
    compile
    link(FastOptJS, fastOptJSFile)
    fastOptJSFile
  }
  def fullOptJS = {
    compile
    link(FullOptJS, fullOptJSFile)
    fullOptJSFile
  }
}
