package cbt
import java.io.File
trait Scalameta extends BaseBuild{
  def scalaMetaVersion: String = "1.6.0"

  override def scalacOptions = super.scalacOptions ++ Scalameta.scalacOptions(
    Scalameta.scalaHost( scalaVersion, scalaMetaVersion, context.cbtLastModified, context.paths.mavenCache ).jar
  )
}
object Scalameta{
  def scalaHost(
    scalaVersion: String, scalaMetaVersion: String, cbtLastModified: Long, mavenCache: java.io.File
  )(
    implicit logger: Logger, transientCache: java.util.Map[AnyRef,AnyRef], classLoaderCache: ClassLoaderCache
  ) =
    MavenResolver( cbtLastModified, mavenCache, mavenCentral ).bindOne(
      MavenDependency(
        "org.scalameta", "scalahost_"+scalaVersion, scalaMetaVersion
      )
    )

  def scalacOptions( scalaHost: File ) =
    Seq(
      "-Xplugin:" ++ scalaHost.string,
      "-Yrangepos",
      "-Xplugin-require:scalahost"
    )
}
