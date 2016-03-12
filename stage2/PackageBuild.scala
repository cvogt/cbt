package cbt
import java.io.File
import java.net.URL
import scala.collection.immutable.Seq
abstract class PackageBuild(context: Context) extends BasicBuild(context) with ArtifactInfo{
  def `package`: Seq[File] = lib.concurrently( enableConcurrency )(
    Seq(() => jar, () => docJar, () => srcJar)
  )( _() )

  private object cacheJarBasicBuild extends Cache[File]
  def jar: File = cacheJarBasicBuild{
    lib.jar( artifactId, version, compile, jarTarget )
  }

  private object cacheSrcJarBasicBuild extends Cache[File]
  def srcJar: File = cacheSrcJarBasicBuild{
    lib.srcJar( sourceFiles, artifactId, version, scalaTarget )
  }

  private object cacheDocBasicBuild extends Cache[File]
  def docJar: File = cacheDocBasicBuild{
    lib.docJar( scalaVersion, sourceFiles, dependencyClasspath, apiTarget, jarTarget, artifactId, version, scalacOptions )
  }

  override def jars = jar +: dependencyJars
  override def exportedJars: Seq[File] = Seq(jar)
}
