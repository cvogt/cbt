package cbt
import java.io.File

case class ScalaJsLib(
  scalaJsVersion: String, scalaVersion: String, cbtLastModified: Long, mavenCache: File
)(implicit logger: Logger, transientCache: java.util.Map[AnyRef,AnyRef], classLoaderCache: ClassLoaderCache){
  val lib = new Lib(logger)
  def dep(artifactId: String) = MavenResolver( cbtLastModified, mavenCache, mavenCentral ).bindOne(
    MavenDependency("org.scala-js", artifactId, scalaJsVersion)
  )

  def link(
    outputPath: File, scalaJsOptions: Seq[String], entriesToLink: Seq[File]
  ): ExitCode = {
    outputPath.getParentFile.mkdirs
    val scalaJsCliDep = dep( "scalajs-cli_"++lib.libMajorVersion(scalaVersion) )
    outputPath.getParentFile.mkdirs
    lib.runMain(
      "org.scalajs.cli.Scalajsld",
      Seq(
        "--sourceMap",
        "--stdlib", s"${scalaJsLibraryDependency.jar.getAbsolutePath}",
        "--output", outputPath.string
      ) ++ scalaJsOptions ++ entriesToLink.map(_.getAbsolutePath),
      scalaJsCliDep.classLoader
    )
  }

  val scalaJsLibraryDependency = dep( "scalajs-library_"++lib.libMajorVersion(scalaVersion) )

  // Has to be full Scala version because the compiler is incompatible between versions
  val scalaJsCompilerDependency = dep( "scalajs-compiler_"++scalaVersion )  
  val scalacOptions = Seq(
    "-Xplugin:" ++ scalaJsCompilerDependency.jar.string,
    "-Xplugin-require:scalajs"
  )
}
