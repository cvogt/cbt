package cbt
import cbt.paths._

import java.io._
import java.lang.reflect.InvocationTargetException
import java.net._
import java.nio.file.{Path =>_,_}
import java.nio.file.Files.readAllBytes
import java.security.MessageDigest
import java.util.jar._

import scala.collection.immutable.Seq
import scala.reflect.runtime.{universe => ru}
import scala.util._

import ammonite.ops.{cwd => _,_}

class BasicBuild( context: Context ) extends Build( context )
class Build(val context: Context) extends Dependency with TriggerLoop{
  // library available to builds
  implicit final val logger: Logger = context.logger
  override final protected val lib: Lib = new Lib(logger)

  // ========== general stuff ==========

  def enableConcurrency = false
  final def projectDirectory: File = lib.realpath(context.cwd)
  assert( projectDirectory.exists, "projectDirectory does not exist: " ++ projectDirectory.string )
  final def usage: Unit = new lib.ReflectBuild(this).usage

  // ========== meta data ==========

  def scalaVersion: String = constants.scalaVersion
  final def scalaMajorVersion: String = lib.scalaMajorVersion(scalaVersion)
  def zincVersion = "0.3.9"

  def dependencies: Seq[Dependency] = Seq(
    "org.scala-lang" % "scala-library" % scalaVersion
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

  /** Source directories and files. Defaults to .scala and .java files in src/ and top-level. */
  def sources: Seq[File] = Seq(defaultSourceDirectory) ++ projectDirectory.listFiles.toVector.filter(sourceFileFilter)

  /** Which file endings to consider being source files. */
  def sourceFileFilter(file: File): Boolean = file.toString.endsWith(".scala") || file.toString.endsWith(".java")

  /** Absolute path names for all individual files found in sources directly or contained in directories. */
  final def sourceFiles: Seq[File] = for {
    base <- sources.filter(_.exists).map(lib.realpath)
    file <- lib.listFilesRecursive(base) if file.isFile && sourceFileFilter(file)
  } yield file

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

  /** SBT-like dependency builder DSL for syntax compatibility */
  class DependencyBuilder2( groupId: String, artifactId: String, scalaVersion: Option[String] ){
    def %(version: String) = scalaVersion.map(
      v => ScalaDependency(groupId, artifactId, version, scalaVersion = v)
    ).getOrElse(
      JavaDependency(groupId, artifactId, version)
    )
  }
  implicit class DependencyBuilder(groupId: String){
    def %%(artifactId: String) = new DependencyBuilder2( groupId, artifactId, Some(scalaMajorVersion) )
    def  %(artifactId: String) = new DependencyBuilder2( groupId, artifactId, None )
  }
  implicit class DependencyBuilder3(d: JavaDependency){
    def  %(classifier: String) = d.copy(classifier = Classifier(Some(classifier)))
  }

  final def BuildDependency(path: File) = cbt.BuildDependency(
    context.copy( cwd = path, args = Seq() )
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

  def exportedClasspath   : ClassPath = ClassPath(Seq(compile))
  def targetClasspath = ClassPath(Seq(compileTarget))
  def exportedJars: Seq[File] = Seq()
  // ========== compile, run, test ==========

  /** scalac options used for zinc and scaladoc */
  def scalacOptions: Seq[String] = Seq( "-feature", "-deprecation", "-unchecked" )

  val updated: Boolean = {
    val existingClassFiles = lib.listFilesRecursive(compileTarget)
    val sourcesChanged = existingClassFiles.nonEmpty && {
      val oldestClassFile = existingClassFiles.sortBy(_.lastModified).head
      val oldestClassFileAge = oldestClassFile.lastModified
      val changedSourceFiles = sourceFiles.filter(_.lastModified > oldestClassFileAge)
      if(changedSourceFiles.nonEmpty){
        /*
        println(changedSourceFiles)
        println(changedSourceFiles.map(_.lastModified))
        println(changedSourceFiles.map(_.lastModified > oldestClassFileAge))
        println(oldestClassFile)
        println(oldestClassFileAge)
        println("-"*80)
        */
      }
      changedSourceFiles.nonEmpty
    }
    sourcesChanged || transitiveDependencies.map(_.updated).fold(false)(_ || _)
  }

  private object compileCache extends Cache[File]
  def compile: File = compileCache{
    lib.compile(
      updated,
      sourceFiles, compileTarget, dependencyClasspath, scalacOptions,
      zincVersion = zincVersion, scalaVersion = scalaVersion
    )
  }

  def runClass: String = "Main"
  def run: ExitCode = lib.runMainIfFound( runClass, context.args, classLoader )

  def test: ExitCode = lib.test(context)

  context.logger.composition(">"*80)
  context.logger.composition("class   " ++ this.getClass.toString)
  context.logger.composition("dir     " ++ projectDirectory.string)
  context.logger.composition("sources " ++ sources.toList.mkString(" "))
  context.logger.composition("target  " ++ target.string)
  context.logger.composition("context " ++ context.toString)
  context.logger.composition("dependencyTree\n" ++ dependencyTree)
  context.logger.composition("<"*80)

  // ========== cbt internals ==========
  private[cbt] def finalBuild = this
  override def show = this.getClass.getSimpleName ++ "(" ++ projectDirectory.string ++ ")"
}
