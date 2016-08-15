package cbt
import java.nio._
import java.nio.file._
// would love to call this just `Package` but that conflicts with scala package objects.
trait PackageJars extends BaseBuild with ArtifactInfo{
  def name: String
  def artifactId = name
  def defaultVersion: String
  final def version = context.version getOrElse defaultVersion
  def `package`: Seq[Path] = lib.concurrently( enableConcurrency )(
    Seq(() => jar, () => docJar, () => srcJar)
  )( _() ).flatten

  private object cacheJarBasicBuild extends Cache[Option[Path]]
  def jar: Option[Path] = cacheJarBasicBuild{
    compile.flatMap( lib.jar( artifactId, scalaMajorVersion, version, _, jarTarget ) )
  }

  private object cacheSrcJarBasicBuild extends Cache[Option[Path]]
  def srcJar: Option[Path] = cacheSrcJarBasicBuild{
    lib.srcJar( sourceFiles, artifactId, scalaMajorVersion, version, scalaTarget )
  }

  private object cacheDocBasicBuild extends Cache[Option[Path]]
  def docJar: Option[Path] = cacheDocBasicBuild{
    lib.docJar(
      context.cbtHasChanged,
      scalaVersion, sourceFiles, dependencyClasspath, apiTarget,
      jarTarget, artifactId, scalaMajorVersion, version,
      scalacOptions, context.classLoaderCache, context.paths.mavenCache
    )
  }
}
