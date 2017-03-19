package cbt
import java.io._
import java.nio.file._
import java.nio.file.Files._
import java.net._
import java.lang.reflect._

object `package`{
  implicit class CbtExitCodeOps( val exitCode: ExitCode ) extends AnyVal with common_1.ops.CbtExitCodeOps
  implicit class TypeInferenceSafeEquals[T]( val value: T ) extends AnyVal with common_1.ops.TypeInferenceSafeEquals[T]
  implicit class CbtBooleanOps( val condition: Boolean ) extends AnyVal with common_1.ops.CbtBooleanOps
  implicit class CbtStringOps( val string: String ) extends AnyVal with common_1.ops.CbtStringOps

  implicit class CbtFileOps( val file: File ) extends file.ops.CbtFileOps

  implicit class CbtClassOps( val c: Class[_] ) extends AnyVal with reflect.ops.CbtClassOps
  implicit class CbtConstructorOps( val c: Constructor[_] ) extends AnyVal with reflect.ops.CbtConstructorOps
  implicit class CbtMethodOps( val m: Method ) extends AnyVal with reflect.ops.CbtMethodOps

  val mavenCentral = new URL("https://repo1.maven.org/maven2")
  val jcenter = new URL("https://jcenter.bintray.com")
  def bintray(owner: String) = new URL(s"""https://dl.bintray.com/${URLEncoder.encode(owner, "UTF-8")}/maven""")
  private val sonatypeBase  = new URL("https://oss.sonatype.org/content/repositories/")
  val sonatypeReleases = sonatypeBase ++ "releases"
  val sonatypeSnapshots = sonatypeBase ++ "snapshots"

  implicit class PathExtensionMethods( path: Path ){
    def /(s: String): Path = path.resolve(s)
    def ++( s: String ): Path = {
      if(s endsWith "/") throw new Exception(
        """Trying to append a String that ends in "/" to a Path would loose the trailing "/". Use .stripSuffix("/") if you need to."""
      )
      Paths.get( path.toString ++ s )
    }
  }

  implicit class URLExtensionMethods( url: URL ){
    def ++( s: String ): URL = new URL( url.toString ++ s )
    def show = "/[^/@]+@".r.replaceFirstIn( url.toString, "/" ) // remove credentials when showing url for security reasons
  }
  implicit class SeqExtensions[T](seq: Seq[T]){
     def maxOption(implicit ev: Ordering[T]): Option[T] = try{ Some(seq.max) } catch {
       case e:java.lang.UnsupportedOperationException if e.getMessage === "empty.max" => None
     }
  }
  implicit class ClassLoaderExtensions(classLoader: ClassLoader){
    def canLoad(className: String) = {
      try{
        classLoader.loadClass(className)
        true
      } catch {
        case e: ClassNotFoundException => false
      }
    }
  }
  implicit class ArtifactInfoExtensions(subject: ArtifactInfo){
    import subject._
    def str = s"$groupId:$artifactId:$version"
    def show = this.getClass.getSimpleName ++ s"($str)"
  }
  implicit class DependencyExtensions(subject: Dependency){
    import subject._
    def dependencyClasspath(implicit logger: Logger, transientCache: java.util.Map[AnyRef,AnyRef], classLoaderCache: ClassLoaderCache): ClassPath
      = Dependencies(dependenciesArray.toVector).classpath
    def exportedClasspath: ClassPath = ClassPath(exportedClasspathArray.toVector)
    def classpath(implicit logger: Logger, transientCache: java.util.Map[AnyRef,AnyRef], classLoaderCache: ClassLoaderCache) = exportedClasspath ++ dependencyClasspath
    def dependencies: Seq[Dependency] = dependenciesArray.toVector
  }
  implicit class ContextExtensions(subject: Context){
    import subject._
    val paths = CbtPaths(cbtHome, cache)
    implicit def logger: Logger = new Logger(enabledLoggers, start)

    def classLoaderCache: ClassLoaderCache = new ClassLoaderCache( persistentCache )
    def cbtDependencies = {
      import paths._
      new CbtDependencies(mavenCache, nailgunTarget, stage1Target, stage2Target, compatibilityTarget)(logger, transientCache, classLoaderCache)
    }
    val cbtDependency = cbtDependencies.stage2Dependency

    def args: Seq[String] = argsArray.toVector
    def enabledLoggers: Set[String] = enabledLoggersArray.toSet
    def scalaVersion = Option(scalaVersionOrNull)
    def parentBuild = Option(parentBuildOrNull)
    def cbtLastModified: scala.Long = subject.cbtLastModified

    def copy(
      workingDirectory: File = workingDirectory,
      args: Seq[String] = args,
      //enabledLoggers: Set[String] = enabledLoggers,
      cbtLastModified: Long = cbtLastModified,
      scalaVersion: Option[String] = scalaVersion,
      cbtHome: File = cbtHome,
      parentBuild: Option[BuildInterface] = None,
      transientCache: java.util.Map[AnyRef,AnyRef] = transientCache,
      persistentCache: java.util.Map[AnyRef,AnyRef] = persistentCache,
      loop: Boolean = loop
    ): Context = new ContextImplementation(
      workingDirectory,
      cwd,
      args.to,
      enabledLoggers.to,
      start,
      cbtLastModified,
      scalaVersion.getOrElse(null),
      persistentCache,
      transientCache,
      cache,
      cbtHome,
      cbtRootHome,
      compatibilityTarget,
      parentBuild.getOrElse(null),
      loop
    )
  }
}

