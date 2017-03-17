package cbt
import java.io.File

// would love to call this just `Package` but that conflicts with scala package objects.
trait PackageJars extends BaseBuild with ArtifactInfo{
  def name: String
  def artifactId = name
  def version: String
  def `package`: Seq[File] = lib.concurrently( enableConcurrency )(
    Seq(() => jar, () => docJar, () => srcJar)
  )( _() ).flatten

  def jarFilePrefix = artifactId++"_"++scalaMajorVersion++"-"++version

  def jar: Option[File] = taskCache[PackageJars]("jar").memoize{
    lib.createJar( jarTarget / jarFilePrefix++".jar", exportedClasspath.files )
  }

  def srcJar: Option[File] = taskCache[PackageJars]("srcJar").memoize{
    lib.createJar(
      jarTarget / jarFilePrefix++"-sources.jar", nonEmptySourceFiles
    )
  }

  def docJar: Option[File] = taskCache[PackageJars]("docJar").memoize{
    lib.createJar( jarTarget / jarFilePrefix++"-javadoc.jar", scaladoc.toSeq )
  }
}
