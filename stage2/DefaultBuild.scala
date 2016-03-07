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




abstract class PackageBuild(context: Context) extends Build(context) with ArtifactInfo{
  def `package`: Seq[File] = lib.concurrently( enableConcurrency )(
    Seq(() => jar, () => docJar, () => srcJar)
  )( _() )

  private object cacheJarBasicBuild extends Cache[File]
  def jar: File = cacheJarBasicBuild{
    lib.jar( artifactId, version, compile, jarTarget )
  }

  private object cacheSrcJarBasicBuild extends Cache[File]
  def srcJar: File = cacheSrcJarBasicBuild{
    lib.srcJar(sources, artifactId, version, scalaTarget)
  }

  private object cacheDocBasicBuild extends Cache[File]
  def docJar: File = cacheDocBasicBuild{
    lib.docJar( sources, dependencyClasspath, apiTarget, jarTarget, artifactId, version, scalacOptions )
  }

  override def jars = jar +: dependencyJars
  override def exportedJars: Seq[File] = Seq(jar)
}
abstract class PublishBuild(context: Context) extends PackageBuild(context){
  def name = artifactId
  def description: String
  def url: URL
  def developers: Seq[Developer]
  def licenses: Seq[License]
  def scmUrl: String
  def scmConnection: String
  def pomExtra: Seq[scala.xml.Node] = Seq()

  // ========== package ==========

  /** put additional xml that should go into the POM file in here */
  def pom: File = lib.pom(
    groupId = groupId,
    artifactId = artifactId,
    version = version,
    name = name,
    description = description,
    url = url,
    developers = developers,
    licenses = licenses,
    scmUrl = scmUrl,
    scmConnection = scmConnection,
    dependencies = dependencies,
    pomExtra = pomExtra,
    jarTarget = jarTarget
  )

  // ========== publish ==========
  final protected def releaseFolder = s"/${groupId.replace(".","/")}/$artifactId/$version/"
  def snapshotUrl = new URL("https://oss.sonatype.org/content/repositories/snapshots")
  def releaseUrl = new URL("https://oss.sonatype.org/service/local/staging/deploy/maven2")
  def publishSnapshot: Unit = lib.publishSnapshot(sourceFiles, pom +: `package`, new URL(snapshotUrl + releaseFolder) )
  def publishSigned: Unit = lib.publishSigned(sourceFiles, pom +: `package`, new URL(releaseUrl + releaseFolder) )
}


class BasicBuild(context: Context) extends Build(context)
class Build(val context: Context) extends Dependency with TriggerLoop{
  // library available to builds
  final val logger = context.logger
  override final protected val lib: Lib = new Lib(logger)
  // ========== general stuff ==========

  def enableConcurrency = false
  final def projectDirectory: File = new File(context.cwd)
  assert( projectDirectory.exists, "projectDirectory does not exist: "+projectDirectory )
  final def usage: Unit = new lib.ReflectBuild(this).usage
/*
  def scaffold: Unit = lib.generateBasicBuildFile(
    projectDirectory, scalaVersion, groupId, artifactId, version
  )
*/
  // ========== meta data ==========

  def scalaVersion: String = constants.scalaVersion
  final def scalaMajorVersion: String = scalaVersion.split("\\.").take(2).mkString(".")
  def zincVersion = "0.3.9"

  def dependencies: Seq[Dependency] = Seq(
    "org.scala-lang" % "scala-library" % scalaVersion
  )

  // ========== paths ==========
  final private val defaultSourceDirectory = new File(projectDirectory+"/src/")

  /** base directory where stuff should be generated */
  def target = new File(projectDirectory+"/target")
  /** base directory where stuff should be generated for this scala version*/
  def scalaTarget = new File(target + s"/scala-$scalaMajorVersion")
  /** directory where jars (and the pom file) should be put */
  def jarTarget = scalaTarget
  /** directory where the scaladoc should be put */
  def apiTarget = new File(scalaTarget + "/api")
  /** directory where the class files should be put (in package directories) */
  def compileTarget = new File(scalaTarget + "/classes")

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
      "Some sources do not exist: \n"+nonExisting.mkString("\n")
    )
  }
  assertSourceDirectories()




  /** SBT-like dependency builder DSL */
  class GroupIdAndArtifactId( groupId: String, artifactId: String ){
    def %(version: String) = new MavenDependency(groupId, artifactId, version)(lib.logger)
  }
  implicit class DependencyBuilder(groupId: String){
    def %%(artifactId: String) = new GroupIdAndArtifactId( groupId, artifactId+"_"+scalaMajorVersion )
    def  %(artifactId: String) = new GroupIdAndArtifactId( groupId, artifactId )
  }

  final def BuildDependency(path: String) = cbt.BuildDependency(
    context.copy(
      cwd = path,
      args = Seq()
    )
  )

  def triggerLoopFiles: Seq[File] = sources ++ transitiveDependencies.collect{ case b: TriggerLoop => b.triggerLoopFiles }.flatten
  
  
  def localJars           : Seq[File] =
    Seq(projectDirectory + "/lib/")
      .map(new File(_))
      .filter(_.exists)
      .flatMap(_.listFiles)
      .filter(_.toString.endsWith(".jar"))

  //def cacheJar = false
  override def dependencyClasspath : ClassPath = ClassPath(localJars) ++ super.dependencyClasspath
  override def dependencyJars      : Seq[File] = localJars ++ super.dependencyJars

  def exportedClasspath   : ClassPath = ClassPath(Seq(compile))
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
 
  private object cacheCompileBasicBuild extends Cache[File]
  def compile: File = cacheCompileBasicBuild{
    //println(transitiveDependencies.filter(_.updated).mkString("\n"))
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
  context.logger.composition("class   "+this.getClass)
  context.logger.composition("dir     "+context.cwd)
  context.logger.composition("sources "+sources.toList.mkString(" "))
  context.logger.composition("target  "+target)
  context.logger.composition("context "+context)
  context.logger.composition("dependencyTree\n"+dependencyTree)
  context.logger.composition("<"*80)

  // ========== cbt internals ==========
  private[cbt] def finalBuild = this
  override def show = this.getClass.getSimpleName + "("+context.cwd+")"
}
