package cbt

import java.io._
import java.net._
import java.nio.file._
import java.nio.file.Files.readAllBytes
import java.security.MessageDigest
import java.util.jar._

import scala.util._
import scala.collection.JavaConverters._

class BasicBuild(val context: Context) extends BaseBuild
trait BaseBuild extends DependencyImplementation with BuildInterface with TriggerLoop with SbtDependencyDsl{
  def context: Context
  
  // library available to builds
  implicit protected final val logger: Logger = context.logger
  implicit protected final val classLoaderCache: ClassLoaderCache = context.classLoaderCache
  implicit protected final val _context = context
  override protected final val lib: Lib = new Lib(logger)

  // ========== general stuff ==========

  def enableConcurrency = false
  final def projectDirectory: Path = lib.realpath(context.projectDirectory)
  assert( Files.exists( projectDirectory, LinkOption.NOFOLLOW_LINKS ), "projectDirectory does not exist: " ++ projectDirectory.toString )
  final def usage: String = lib.usage(this.getClass, show)

  // ========== meta data ==========

  def defaultScalaVersion: String = constants.scalaVersion
  final def scalaVersion = context.scalaVersion getOrElse defaultScalaVersion
  final def scalaMajorVersion: String = lib.libMajorVersion(scalaVersion)
  def crossScalaVersions: Seq[String] = Seq(scalaVersion, "2.10.6")
  final def crossScalaVersionsArray: Array[String] = crossScalaVersions.to
  def projectName = "default"

  // TODO: this should probably provide a nice error message if class has constructor signature
  def copy(context: Context): BuildInterface = lib.copy(this.getClass, context).asInstanceOf[BuildInterface]
  def zincVersion = "0.3.9"

  def dependencies: Seq[Dependency] =
    // FIXME: this should probably be removed
    Resolver( mavenCentral ).bind(
      "org.scala-lang" % "scala-library" % scalaVersion
    )

  // ========== paths ==========
  final private val defaultSourceDirectory = projectDirectory ++ "src"

  /** base directory where stuff should be generated */
  def target: Path = projectDirectory ++ "target"
  /** base directory where stuff should be generated for this scala version*/
  def scalaTarget: Path = target ++ s"scala-$scalaMajorVersion"
  /** directory where jars (and the pom file) should be put */
  def jarTarget: Path = scalaTarget
  /** directory where the scaladoc should be put */
  def apiTarget: Path = scalaTarget ++ "api"
  /** directory where the class files should be put (in package directories) */
  def compileTarget: Path = scalaTarget ++ "classes"
  /**
  File which cbt uses to determine if it needs to trigger an incremental re-compile.
  Last modified date is the time when the last successful compilation started.
  Contents is the cbt version git hash.
  */
  def compileStatusFile: Path = Paths.get(compileTarget.toString + ".last-success")

  /** Source directories and files. Defaults to .scala and .java files in src/ and top-level. */
  def sources: Seq[Path] = Seq(defaultSourceDirectory) ++ Files.newDirectoryStream(projectDirectory).iterator.asScala.filter(lib.sourceFileFilter)

  /** Absolute path names for all individual files found in sources directly or contained in directories. */
  final def sourceFiles: Seq[Path] = lib.sourceFiles(sources)

  protected def logEmptySourceDirectories(): Unit = {
    val nonExisting =
      sources
        .filterNot( f => Files.exists(f, LinkOption.NOFOLLOW_LINKS) )
        .diff( Seq(defaultSourceDirectory) )
    if(!nonExisting.isEmpty) logger.stage2("Some sources do not exist: \n"++nonExisting.mkString("\n"))
  }
  logEmptySourceDirectories()

  def Resolver( urls: URL* ) = MavenResolver( context.cbtHasChanged, context.paths.mavenCache, urls: _* )

  def ScalaDependency(
    groupId: String, artifactId: String, version: String, classifier: Classifier = Classifier.none,
    scalaVersion: String = scalaMajorVersion
  ) = lib.ScalaDependency( groupId, artifactId, version, classifier, scalaVersion )

  final def DirectoryDependency(path: Path) = cbt.DirectoryDependency(
    context.copy( projectDirectory = path, args = Seq() )
  )

  def triggerLoopFiles: Seq[Path] = sources ++ transitiveDependencies.collect{ case b: TriggerLoop => b.triggerLoopFiles }.flatten

  def localJars           : Seq[Path] =
    Seq(projectDirectory ++ "lib")
      .filter(f => Files.exists(f, LinkOption.NOFOLLOW_LINKS) )
      .flatMap( f => Files.newDirectoryStream(f).iterator.asScala.toSeq )
      .filter(_.toString.endsWith(".jar"))

  override def dependencyClasspath : ClassPath = ClassPath(localJars) ++ super.dependencyClasspath

  protected def compileDependencies: Seq[Dependency] = Nil
  final def compileClasspath : ClassPath = ClassPath( compileDependencies.flatMap(_.exportedClasspath.files).distinct )

  def exportedClasspath   : ClassPath = ClassPath(compile.toSeq)
  def targetClasspath = ClassPath(Seq(compileTarget))
  // ========== compile, run, test ==========

  /** scalac options used for zinc and scaladoc */
  def scalacOptions: Seq[String] = Seq(
    "-feature",
    "-deprecation",
    "-unchecked"
  )

  private object needsUpdateCache extends Cache[Boolean]
  def needsUpdate: Boolean = needsUpdateCache(
    context.cbtHasChanged
    || lib.needsUpdate( sourceFiles, compileStatusFile )
    || transitiveDependencies.filterNot(_ == context.parentBuild).exists(_.needsUpdate)
  )

  private object compileCache extends Cache[Option[Path]]
  def compile: Option[Path] = compileCache{
    lib.compile(
      context.cbtHasChanged,
      needsUpdate || context.parentBuild.map(_.needsUpdate).getOrElse(false),
      sourceFiles, compileTarget, compileStatusFile, dependencyClasspath ++ compileClasspath,
      context.paths.mavenCache, scalacOptions, context.classLoaderCache,
      zincVersion = zincVersion, scalaVersion = scalaVersion
    )
  }

  def runClass: String = "Main"
  def run: ExitCode = lib.runMainIfFound( runClass, context.args, classLoader(context.classLoaderCache) )

  def test: Option[ExitCode] = 
    Some(new lib.ReflectBuild(
      DirectoryDependency(projectDirectory++"/test").build
    ).callNullary(Some("run")))
  def t = test
  def rt = recursiveUnsafe(Some("test"))

  def recursiveSafe(_run: BuildInterface => Any): ExitCode = {
    val builds = (this +: transitiveDependencies).collect{
      case b: BuildInterface => b
    }
    val results = builds.map(_run)
    if(
      results.forall{
        case Some(_:ExitCode) => true
        case None => true
        case _:ExitCode => true
        case other => false
      }
    ){
      if(
        results.collect{
          case Some(c:ExitCode) => c
          case c:ExitCode => c
        }.filter(_ != 0)
         .nonEmpty
      ) ExitCode.Failure
      else ExitCode.Success
    } else ExitCode.Success
  }

  def recursive: ExitCode = {
    recursiveUnsafe(context.args.lift(1))
  }

  def recursiveUnsafe(taskName: Option[String]): ExitCode = {
    recursiveSafe{
      b =>
      System.err.println(b.show)
      lib.trapExitCode{ // FIXME: trapExitCode does not seem to work here
        try{
          new lib.ReflectBuild(b).callNullary(taskName)
          ExitCode.Success
        } catch {
          case e: Throwable => println(e.getClass); throw e
        }        
      }
      ExitCode.Success
    }
  }

  def c = compile
  def r = run

  /*
  context.logger.composition(">"*80)
  context.logger.composition("class   " ++ this.getClass.toString)
  context.logger.composition("dir     " ++ projectDirectory.string)
  context.logger.composition("sources " ++ sources.toList.mkString(" "))
  context.logger.composition("target  " ++ target.string)
  context.logger.composition("context " ++ context.toString)
  context.logger.composition("dependencyTree\n" ++ dependencyTree)
  context.logger.composition("<"*80)
  */

  // ========== cbt internals ==========
  def finalBuild: BuildInterface = this
  override def show = this.getClass.getSimpleName ++ "(" ++ projectDirectory.string ++ ")"
}
