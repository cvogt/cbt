package cbt
import java.io._
import java.nio.file._
import java.net._
import scala.collection.immutable.Seq
import java.util.concurrent.ConcurrentHashMap

object `package`{
  private val lib = new BaseLib
  implicit class FileExtensionMethods( file: File ){
    def ++( s: String ): File = {
      if(s endsWith "/") throw new Exception(
        """Trying to append a String that ends in "/" to a File would loose it. Use .stripSuffix("/") if you need to."""
      )
      new File( file.toString ++ s )
    }
    def parent = lib.realpath(file ++ "/..")
    def string = file.toString
  }
  implicit class URLExtensionMethods( url: URL ){
    def ++( s: String ): URL = new URL( url.toString ++ s )
    def string = url.toString
  }
  implicit class BuildInterfaceExtensions(build: BuildInterface){
    import build._
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
    val paths = Paths(cbtHome, cache)
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
      projectDirectory: File = projectDirectory,
      args: Seq[String] = args,
      enabledLoggers: Set[String] = enabledLoggers,
      cbtHasChanged: Boolean = cbtHasChanged,
      version: Option[String] = version,
      scalaVersion: Option[String] = scalaVersion,
      cache: File = cache,
      cbtHome: File = cbtHome,
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
      compatibilityTarget,
      parentBuild.getOrElse(null)
    )
  }
}

