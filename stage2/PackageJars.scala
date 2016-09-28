package cbt
import java.io.File

// would love to call this just `Package` but that conflicts with scala package objects.
trait PackageJars extends BaseBuild with ArtifactInfo{
  def name: String
  // TODO: why not final?
  def artifactId = name
  def defaultVersion: String
  final def version = context.version getOrElse defaultVersion
  def `package`: Seq[File] = lib.concurrently( enableConcurrency )(
    Seq(() => jar, () => docJar, () => srcJar)
  )( _() ).flatten

  private object cacheJarBasicBuild extends Cache[Option[File]]
  def jar: Option[File] = cacheJarBasicBuild{
    compile.flatMap( lib.jar( artifactId, scalaMajorVersion, version, _, jarTarget ) )
  }

  private object cacheSrcJarBasicBuild extends Cache[Option[File]]
  def srcJar: Option[File] = cacheSrcJarBasicBuild{
    lib.srcJar( sourceFiles, artifactId, scalaMajorVersion, version, scalaTarget )
  }

  private object cacheDocBasicBuild extends Cache[Option[File]]
  def docJar: Option[File] = cacheDocBasicBuild{
    lib.docJar(
      context.cbtHasChanged,
      scalaVersion, sourceFiles, compileClasspath, docTarget,
      jarTarget, artifactId, scalaMajorVersion, version,
      scalacOptions, context.classLoaderCache, context.paths.mavenCache
    )
  }
}
