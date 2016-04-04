package cbt
import java.io.File
import scala.collection.immutable.Seq
abstract class PackageBuild(context: Context) extends BasicBuild(context) with ArtifactInfo{
  def `package`: Seq[File] = lib.concurrently( enableConcurrency )(
    Seq(() => jar, () => docJar, () => srcJar)
  )( _() ).flatten

  private object cacheJarBasicBuild extends Cache[Option[File]]
  def jar: Option[File] = cacheJarBasicBuild{
    compile.flatMap( lib.jar( artifactId, version, _, jarTarget ) )
  }

  private object cacheSrcJarBasicBuild extends Cache[Option[File]]
  def srcJar: Option[File] = cacheSrcJarBasicBuild{
    lib.srcJar( sourceFiles, artifactId, version, scalaTarget )
  }

  private object cacheDocBasicBuild extends Cache[Option[File]]
  def docJar: Option[File] = cacheDocBasicBuild{
    lib.docJar( scalaVersion, sourceFiles, dependencyClasspath, apiTarget, jarTarget, artifactId, version, scalacOptions, context.classLoaderCache )
  }

  override def jars = jar.toVector ++ dependencyJars
  override def exportedJars: Seq[File] = jar.toVector
}
