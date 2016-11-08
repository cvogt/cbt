package cbt

import java.io._
import java.net._

class BasicBuild(val context: Context) extends BaseBuild
trait BaseBuild extends BuildInterface with DependencyImplementation with TriggerLoop with SbtDependencyDsl{
  def context: Context
  
  // library available to builds
  implicit protected final val logger: Logger = context.logger
  implicit protected final val classLoaderCache: ClassLoaderCache = context.classLoaderCache
  implicit protected final val _context = context
  override protected final val lib: Lib = new Lib(logger)

  // ========== general stuff ==========

  def enableConcurrency = false
  final def projectDirectory: File = lib.realpath(context.projectDirectory)
  assert( projectDirectory.exists, "projectDirectory does not exist: " ++ projectDirectory.string )
  final def usage: String = lib.usage(this.getClass, show)
  
  final def taskNames: String = lib.taskNames(this.getClass).sorted.mkString("\n")

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
  final private val defaultSourceDirectory = projectDirectory ++ "/src"

  /** base directory where stuff should be generated */
  def target: File = projectDirectory ++ "/target"
  /** base directory where stuff should be generated for this scala version*/
  def scalaTarget: File = target ++ s"/scala-$scalaMajorVersion"
  /** directory where jars (and the pom file) should be put */
  def jarTarget: File = scalaTarget
  /** directory where the scaladoc should be put */
  def docTarget: File = scalaTarget ++ "/api"
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

  protected def logEmptySourceDirectories(): Unit = {
    val nonExisting =
      sources
        .filterNot( _.exists )
        .diff( Seq(defaultSourceDirectory) )
    if(!nonExisting.isEmpty) logger.stage2("Some sources do not exist: \n"++nonExisting.mkString("\n"))
  }
  logEmptySourceDirectories()

  def Resolver( urls: URL* ) = MavenResolver( context.cbtHasChanged, context.paths.mavenCache, urls: _* )

  def ScalaDependency(
    groupId: String, artifactId: String, version: String, classifier: Classifier = Classifier.none,
    scalaVersion: String = scalaMajorVersion
  ) = lib.ScalaDependency( groupId, artifactId, version, classifier, scalaVersion )

  final def DirectoryDependency(path: File) = cbt.DirectoryDependency(
    context.copy( projectDirectory = path, args = Seq() )
  )

  def triggerLoopFiles: Seq[File] = sources ++ transitiveDependencies.collect{ case b: TriggerLoop => b.triggerLoopFiles }.flatten

  def localJars           : Seq[File] =
    Seq(projectDirectory ++ "/lib")
      .filter(_.exists)
      .flatMap(_.listFiles)
      .filter(_.toString.endsWith(".jar"))

  override def dependencyClasspath : ClassPath = ClassPath(localJars) ++ super.dependencyClasspath

  protected def compileDependencies: Seq[Dependency] = Nil
  final def compileClasspath : ClassPath =
    dependencyClasspath ++ ClassPath( compileDependencies.flatMap(_.exportedClasspath.files).distinct )

  def resourceClasspath: ClassPath = {
    val resourcesDirectory = projectDirectory ++ "/resources"
      ClassPath( if(resourcesDirectory.exists) Seq(resourcesDirectory) else Nil )
  }
  def exportedClasspath   : ClassPath = ClassPath(compile.toSeq) ++ resourceClasspath
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

  private object compileCache extends Cache[Option[File]]
  def compile: Option[File] = compileCache{
    lib.compile(
      context.cbtHasChanged,
      needsUpdate || context.parentBuild.map(_.needsUpdate).getOrElse(false),
      sourceFiles, compileTarget, compileStatusFile, compileClasspath,
      context.paths.mavenCache, scalacOptions, context.classLoaderCache,
      zincVersion = zincVersion, scalaVersion = scalaVersion
    )
  }

  
  def mainClasses: Seq[Class[_]] = compile.toSeq.flatMap( lib.mainClasses( _, classLoader(classLoaderCache) ) )

  def runClass: Option[String] = lib.runClass( mainClasses ).map( _.getName )

  def run: ExitCode = runClass.map( lib.runMain( _, context.args, classLoader(context.classLoaderCache) ) ).getOrElse{
    logger.task( "No main class found for " ++ projectDirectory.string )
    ExitCode.Success
  }

  def clean: ExitCode = {
    lib.clean(
      target,
      context.args.contains("force"),
      context.args.contains("dry-run"),
      context.args.contains("list"),
      context.args.contains("help")
    )
  }

  def repl: ExitCode = {
    lib.consoleOrFail("Use `cbt direct repl` instead")

    val colorized = "scala.color"
    if(Option(System.getProperty(colorized)).isEmpty) {
      // set colorized REPL, if user didn't pass own value
      System.setProperty(colorized, "true")
    }

    val scalac = new ScalaCompilerDependency(context.cbtHasChanged, context.paths.mavenCache, scalaVersion)
    lib.runMain(
      "scala.tools.nsc.MainGenericRunner",
      Seq(
        "-bootclasspath",
        scalac.classpath.string,
        "-classpath",
        classpath.string
      ) ++ context.args,
      scalac.classLoader(classLoaderCache)
    )
  }

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

  // TODO: allow people not provide the method name, maybe via macro
  /**
  caches given value in context keyed with given key and projectDirectory
  the context is fresh on every complete run of cbt
  */
  def cached[T <: AnyRef](name: String)(task: => T): T = {
    val cache = context.taskCache
    val key = (projectDirectory,name)
    if( cache.containsKey(key) ){
      cache.get(key).asInstanceOf[T]
    } else{
      val value = task
      cache.put( key, value )
      value
    }
  }
}
