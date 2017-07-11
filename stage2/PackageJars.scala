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
    val file = jarTarget / jarFilePrefix++".jar"
    if( file.lastModified < lastModified )
      lib.createJar( file, exportedClasspath.files )
    else Some( file )
  }

  def srcJar: Option[File] = taskCache[PackageJars]("srcJar").memoize{
    val file = jarTarget / jarFilePrefix++"-sources.jar"
    if( file.lastModified < lastModified )
      lib.createJar( file, nonEmptySourceFiles )
    else Some( file )
  }

  def docJar: Option[File] = taskCache[PackageJars]("docJar").memoize{
    val file = jarTarget / jarFilePrefix++"-javadoc.jar"
    if( file.lastModified < lastModified )
      lib.createJar( file, scaladoc.toSeq )
    else Some( file )
  }
}
