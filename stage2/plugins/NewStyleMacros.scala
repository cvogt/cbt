package cbt
import java.io.File

trait NewStyleMacros extends BaseBuild{
  def newStyleMacrosVersion = "3.0.0-M9"

  override def scalacOptions = super.scalacOptions ++ NewStyleMacros.scalacOptions(
    NewStyleMacros.dependencies( scalaVersion, newStyleMacrosVersion, context.cbtLastModified, context.paths.mavenCache ).jar
  )
}

object NewStyleMacros{
  def dependencies(
    scalaVersion: String, newStyleMacrosVersion: String, cbtLastModified: Long, mavenCache: java.io.File
  )(
    implicit logger: Logger, transientCache: java.util.Map[AnyRef,AnyRef], classLoaderCache: ClassLoaderCache
  ) =
    MavenResolver( cbtLastModified, mavenCache, mavenCentral ).bindOne(
      MavenDependency( "org.scalameta", "paradise_"+scalaVersion, newStyleMacrosVersion )
    )

  def scalacOptions( jarPath: File ) =
    Seq(
      "-Xplugin:" ++ jarPath.string,
      "-Yrangepos",
      "-Xplugin-require:macroparadise"
    )
}

