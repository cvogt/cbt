package cbt
import java.io.File

trait Macroparadise extends BaseBuild{
  def macroParadiseVersion = "3.0.0-M9"

  override def scalacOptions = super.scalacOptions ++ Macroparadise.scalacOptions(
    Macroparadise.dependencies( scalaVersion, macroParadiseVersion, context.cbtLastModified, context.paths.mavenCache ).jar
  )
}

object Macroparadise{
  def dependencies(
    scalaVersion: String, macroParadiseVersion: String, cbtLastModified: Long, mavenCache: java.io.File
  )(
    implicit logger: Logger, transientCache: java.util.Map[AnyRef,AnyRef], classLoaderCache: ClassLoaderCache
  ) =
    MavenResolver( cbtLastModified, mavenCache, mavenCentral ).bindOne(
      MavenDependency(
        "org.scalameta", "paradise_"+scalaVersion, macroParadiseVersion
      )
    )

  def scalacOptions( jarPath: File ) =
    Seq(
      "-Xplugin:" ++ jarPath.string,
      "-Yrangepos",
      "-Xplugin-require:macroparadise"
    )
}

