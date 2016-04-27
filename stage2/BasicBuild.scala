package cbt
import cbt.paths._

import java.io._
import java.net._
import java.nio.file.{Path =>_,_}
import java.nio.file.Files.readAllBytes
import java.security.MessageDigest
import java.util.jar._

import scala.collection.immutable.Seq
import scala.util._

trait Recommended extends BasicBuild{
  override def scalacOptions = super.scalacOptions ++ Seq(
    "-feature",
    "-deprecation",
    "-unchecked",
    "-language:postfixOps",
    "-language:implicitConversions",
    "-language:higherKinds",
    "-language:existentials"
  )
}
class BasicBuild( context: Context ) extends Build( context )
class Build(val context: Context) extends Dependency with TriggerLoop with SbtDependencyDsl{
  // library available to builds
  implicit final val logger: Logger = context.logger
  implicit final val classLoaderCache: ClassLoaderCache = context.classLoaderCache
  implicit final val _context = context
  override final protected val lib: Lib = new Lib(logger)

  // ========== general stuff ==========

  override def canBeCached = false
  def enableConcurrency = false
  final def projectDirectory: File = lib.realpath(context.projectDirectory)
  assert( projectDirectory.exists, "projectDirectory does not exist: " ++ projectDirectory.string )
  final def usage: String = lib.usage(this.getClass, context)

  // ========== meta data ==========

  def defaultScalaVersion: String = constants.scalaVersion
  final def scalaVersion = context.scalaVersion getOrElse defaultScalaVersion
  final def scalaMajorVersion: String = lib.scalaMajorVersion(scalaVersion)
  def crossScalaVersions: Seq[String] = Seq(scalaVersion, "2.10.6")
  def copy(context: Context) = lib.copy(this.getClass, context).asInstanceOf[Build]
  def zincVersion = "0.3.9"

  def dependencies: Seq[Dependency] = Seq(
    MavenRepository.central.resolve(
      "org.scala-lang" % "scala-library" % scalaVersion
    )
  )

  // ========== paths ==========
  final private val defaultSourceDirectory = projectDirectory ++ "/src"

  /** base directory where stuff should be generated */
  def target: File = projectDirectory ++ "/target"
  /** base directory where stuff should be generated for this scala version*/
  def scalaTarget: File = target ++ s"/scala-$scalaMajorVersion"
  /** directory where jars (and the pom file) should be put */
  def jarTarget: File = scalaTarget
  /** directory where the scaladoc should be put */
  def apiTarget: File = scalaTarget ++ "/api"
  /** directory where the class files should be put (in package directories) */
  def compileTarget: File = scalaTarget ++ "/classes"
  /**
  File which cbt uses to determine if it needs to trigger an incremental re-compile.
  Last modified date is the time when the last successful compilation started.
  Contents is the cbt version git hash.
  */
  def compileStatusFile: File = compileTarget ++ ".last-success"

  /** Source directories and files. Defaults to .scala and .java files in src/ and top-level. */
  def sources: Seq[File] = Seq(defaultSourceDirectory) ++ projectDirectory.listFiles.toVector.filter(lib.sourceFileFilter)

  /** Absolute path names for all individual files found in sources directly or contained in directories. */
  final def sourceFiles: Seq[File] = lib.sourceFiles(sources)

  protected def assertSourceDirectories(): Unit = {
    val nonExisting =
      sources
        .filterNot( _.exists )
        .diff( Seq(defaultSourceDirectory) )
    assert(
      nonExisting.isEmpty,
      "Some sources do not exist: \n"++nonExisting.mkString("\n")
    )
  }
  assertSourceDirectories()

  def ScalaDependency(
    groupId: String, artifactId: String, version: String, classifier: Classifier = Classifier.none,
    scalaVersion: String = scalaMajorVersion
  ) = lib.ScalaDependency( groupId, artifactId, version, classifier, scalaVersion )

  final def BuildDependency(path: File) = cbt.BuildDependency(
    context.copy( projectDirectory = path, args = Seq() )
  )

  def triggerLoopFiles: Seq[File] = sources ++ transitiveDependencies.collect{ case b: TriggerLoop => b.triggerLoopFiles }.flatten

  def localJars           : Seq[File] =
    Seq(projectDirectory ++ "/lib")
      .filter(_.exists)
      .flatMap(_.listFiles)
      .filter(_.toString.endsWith(".jar"))

  //def cacheJar = false
  override def dependencyClasspath : ClassPath = ClassPath(localJars) ++ super.dependencyClasspath
  override def dependencyJars      : Seq[File] = localJars ++ super.dependencyJars

  def exportedClasspath   : ClassPath = ClassPath(compile.toSeq:_*)
  def targetClasspath = ClassPath(Seq(compileTarget))
  def exportedJars: Seq[File] = Seq()
  // ========== compile, run, test ==========

  /** scalac options used for zinc and scaladoc */
  def scalacOptions: Seq[String] = Seq()

  private object needsUpdateCache extends Cache[Boolean]
  def needsUpdate: Boolean = needsUpdateCache(
    context.cbtHasChanged
    || lib.needsUpdate( sourceFiles, compileStatusFile )
    || transitiveDependencies.exists(_.needsUpdate)
  )

  private object compileCache extends Cache[Option[File]]
  def compile: Option[File] = compileCache{
    lib.compile(
      needsUpdate,
      sourceFiles, compileTarget, compileStatusFile, dependencyClasspath, scalacOptions,
      context.classLoaderCache, zincVersion = zincVersion, scalaVersion = scalaVersion
    )
  }

  def runClass: String = "Main"
  def run: ExitCode = lib.runMainIfFound( runClass, context.args, classLoader(context.classLoaderCache) )

  def test: ExitCode = lib.test(context)

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
  private[cbt] def finalBuild = this
  override def show = this.getClass.getSimpleName ++ "(" ++ projectDirectory.string ++ ")"
}
