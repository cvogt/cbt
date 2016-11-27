package cbt
import java.io._
import java.nio.file._
import java.net._

object `package`{
  implicit class TypeInferenceSafeEquals[T](value: T){
    /** if you don't manually upcast, this will catch comparing different types */
    def ===(other: T) = value == other
    def =!=(other: T) = value != other // =!= instead of !==, because it has better precedence
  }

  val mavenCentral = new URL("https://repo1.maven.org/maven2")
  val jcenter = new URL("https://jcenter.bintray.com")
  def bintray(owner: String) = new URL(s"https://dl.bintray.com/$owner/maven") // FIXME: url encode owner
  private val sonatypeBase  = new URL("https://oss.sonatype.org/content/repositories/")
  val sonatypeReleases = sonatypeBase ++ "releases"
  val sonatypeSnapshots = sonatypeBase ++ "snapshots"

  private val lib = new BaseLib
  implicit class FileExtensionMethods( file: File ){
    def ++( s: String ): File = {
      if(s endsWith "/") throw new Exception(
        """Trying to append a String that ends in "/" to a File would loose the trailing "/". Use .stripSuffix("/") if you need to."""
      )
      new File( file.toString ++ s )
    }
    def /(s: String): File = new File(file.getAbsolutePath + File.separator + s)
    def parent = lib.realpath(file ++ "/..")
    def string = file.toString
  }
  implicit class URLExtensionMethods( url: URL ){
    def ++( s: String ): URL = new URL( url.toString ++ s )
    def string = url.toString
  }
  implicit class BuildInterfaceExtensions(build: BuildInterface){
    import build._
    // TODO: if every build has a method triggers a callback if files change
    // then we wouldn't need this and could provide this method from a 
    // plugin rather than hard-coding trigger files stuff in cbt
    def triggerLoopFiles: Seq[File] = triggerLoopFilesArray.to
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

    def classLoaderCache: ClassLoaderCache = new ClassLoaderCache( logger, persistentCache )
    def cbtDependency = {
      import paths._
      CbtDependency(cbtHasChanged, mavenCache, nailgunTarget, stage1Target, stage2Target, compatibilityTarget)
    }
    def args: Seq[String] = argsArray.to
    def enabledLoggers: Set[String] = enabledLoggersArray.to
    def scalaVersion = Option(scalaVersionOrNull)
    def parentBuild = Option(parentBuildOrNull)
    def start: scala.Long = startCompat
    def cbtHasChanged: scala.Boolean = cbtHasChangedCompat

    def copy(
      projectDirectory: File = projectDirectory,
      args: Seq[String] = args,
      //enabledLoggers: Set[String] = enabledLoggers,
      cbtHasChanged: Boolean = cbtHasChanged,
      scalaVersion: Option[String] = scalaVersion,
      cbtHome: File = cbtHome,
      parentBuild: Option[BuildInterface] = None
    ): Context = ContextImplementation(
      projectDirectory,
      cwd,
      args.to,
      enabledLoggers.to,
      startCompat,
      cbtHasChangedCompat,
      scalaVersion.getOrElse(null),
      persistentCache,
      transientCache,
      cache,
      cbtHome,
      cbtRootHome,
      compatibilityTarget,
      parentBuild.getOrElse(null)
    )
  }
}

