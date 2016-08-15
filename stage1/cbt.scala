package cbt
import java.io._
import java.nio._
import java.nio.file._
import java.net._
import java.util.concurrent.ConcurrentHashMap

import scala.collection.JavaConverters._

object `package`{
  val mavenCentral = new URL("https://repo1.maven.org/maven2")
  val jcenter = new URL("https://jcenter.bintray.com")
  def bintray(owner: String) = new URL(s"https://dl.bintray.com/$owner/maven") // FIXME: url encode owner
  private val sonatypeBase  = new URL("https://oss.sonatype.org/content/repositories/")
  val sonatypeReleases = sonatypeBase ++ "releases"
  val sonatypeSnapshots = sonatypeBase ++ "snapshots"

  private val lib = new BaseLib
  implicit class FileExtensionMethods( path: Path ){
    def ++( s: String ): Path = {
      if(s endsWith "/") throw new Exception(
        """Trying to append a String that ends in "/" to a File would loose the trailing "/". Use .stripSuffix("/") if you need to."""
      )
      Paths.get( path.toString ++ java.io.File.separator ++ s )
    }
    def parent = path.getParent
    def string = path.toString
    def lastModified = Files.getLastModifiedTime( path, LinkOption.NOFOLLOW_LINKS )
    def exists = Files.exists( path, LinkOption.NOFOLLOW_LINKS )
    def isDirectory = Files.isDirectory( path, LinkOption.NOFOLLOW_LINKS )
    def isFile = !isDirectory
    def listFiles = Files.newDirectoryStream(path).iterator.asScala.toSeq
  }
  implicit class URLExtensionMethods( url: URL ){
    def ++( s: String ): URL = new URL( url.toString ++ s )
    def string = url.toString
  }
  implicit class BuildInterfaceExtensions(build: BuildInterface){
    import build._
    def triggerLoopFiles: Seq[Path] = triggerLoopFilesArray.to
    def crossScalaVersions: Seq[String] = crossScalaVersionsArray.to
  }
  implicit class ArtifactInfoExtensions(subject: ArtifactInfo){
    import subject._
    def str = s"$groupId:$artifactId:$version"
    def show = this.getClass.getSimpleName ++ s"($str)"
  }
  implicit class DependencyExtensions(subject: Dependency){
    import subject._
    def dependencyClasspath: ClassPath = ClassPath(dependencyClasspathArray.to)
    def exportedClasspath: ClassPath = ClassPath(exportedClasspathArray.to)
    def classpath = exportedClasspath ++ dependencyClasspath
    def dependencies: Seq[Dependency] = dependenciesArray.to
    def needsUpdate: Boolean = needsUpdateCompat
  }
  implicit class ContextExtensions(subject: Context){
    import subject._
    val paths = CbtPaths(cbtHome, cache)
    implicit def logger: Logger = new Logger(enabledLoggers, start)
    def classLoaderCache: ClassLoaderCache = new ClassLoaderCache(
      logger,
      permanentKeys,
      permanentClassLoaders
    )
    def cbtDependency = {
      import paths._
      CbtDependency(cbtHasChanged, mavenCache, nailgunTarget, stage1Target, stage2Target, compatibilityTarget)
    }
    def args: Seq[String] = argsArray.to
    def enabledLoggers: Set[String] = enabledLoggersArray.to
    def scalaVersion = Option(scalaVersionOrNull)
    def version = Option(versionOrNull)
    def parentBuild = Option(parentBuildOrNull)
    def start: scala.Long = startCompat
    def cbtHasChanged: scala.Boolean = cbtHasChangedCompat

    def copy(
      projectDirectory: Path = projectDirectory,
      args: Seq[String] = args,
      enabledLoggers: Set[String] = enabledLoggers,
      cbtHasChanged: Boolean = cbtHasChanged,
      version: Option[String] = version,
      scalaVersion: Option[String] = scalaVersion,
      cache: Path = cache,
      cbtHome: Path = cbtHome,
      parentBuild: Option[BuildInterface] = None
    ): Context = ContextImplementation(
      projectDirectory,
      cwd,
      args.to,
      enabledLoggers.to,
      startCompat,
      cbtHasChangedCompat,
      version.getOrElse(null),
      scalaVersion.getOrElse(null),
      permanentKeys,
      permanentClassLoaders,
      cache,
      cbtHome,
      cbtRootHome,
      compatibilityTarget,
      parentBuild.getOrElse(null)
    )
  }
}

