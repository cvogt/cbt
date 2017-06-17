package cbt

import java.io._
import java.util._

import scala.collection.JavaConverters._

final case class Stage1ArgsParser(_args: Seq[String]) {
  /**
   * Raw parameters including their `-D` flag.
  **/
  val propsRaw: Seq[String] = _args.toVector.filter(_.startsWith("-D"))

  /**
   * All arguments that weren't `-D` property declarations.
  **/
  val args: Seq[String] = _args.toVector diff propsRaw

  /**
   * Parsed properties, as a map of keys to values.
  **/
  val props = propsRaw
    .map(_.drop(2).split("=")).map({
      case Array(key, value) =>
        key -> value
    }).toMap ++ System.getProperties.asScala

  val enabledLoggers = props.get("log")

  val tools = _args contains "tools"
}


abstract class Stage2Base{
  def run( context: Stage2Args ): ExitCode
}

class Stage2Args(
  val cwd: File,
  val args: Seq[String],
  val stage2LastModified: Long,
  val cache: File,
  val cbtHome: File,
  val compatibilityTarget: File,
  val stage2sourceFiles: Seq[File],
  val loop: Boolean
)(
  implicit val transientCache: java.util.Map[AnyRef,AnyRef], val classLoaderCache: ClassLoaderCache, val logger: Logger
){
  val persistentCache = classLoaderCache.hashMap
}

object Stage1{
  protected def newerThan( a: File, b: File ) ={
    a.lastModified > b.lastModified
  }

  def getBuild( _context: java.lang.Object, buildStage1: BuildStage1Result ) = {
    val context = _context.asInstanceOf[Context]
    val logger = new Logger( context.enabledLoggers, buildStage1.start )
    val (_, cbtLastModified, classLoader) = buildStage2(
      buildStage1,
      context.cbtHome,
      context.cache,
      context.cwd,
      context.loop
    )(context.transientCache, new ClassLoaderCache( context.persistentCache ), logger)

    classLoader
      .loadClass("cbt.Stage2")
      .getMethod( "getBuild", classOf[Context] )
      .invoke(
        null,
        context.copy(
          cbtLastModified = Math.max( context.cbtLastModified, cbtLastModified )
        )
      )
  }

  def buildStage2(
    buildStage1: BuildStage1Result, cbtHome: File, cache: File, cwd: File, loop: Boolean
  )(
    implicit transientCache: java.util.Map[AnyRef,AnyRef], classLoaderCache: ClassLoaderCache, logger: Logger
  ): (Seq[File], Long, ClassLoader) = {

    import buildStage1._

    val lib = new Stage1Lib(logger)
    import lib._
    val paths = CbtPaths(cbtHome, cache)
    import paths._

    val stage2sourceFiles = (
      stage2.listFiles
      ++ (stage2 / "plugins").listOrFail
      ++ (cbtHome / "libraries" / "eval").listOrFail
      ++ (cbtHome / "libraries" / "process").listOrFail
    ).filter(_.isFile).filter(_.toString.endsWith(".scala"))

    val cls = this.getClass.getClassLoader.loadClass("cbt.NailgunLauncher")

    def _cbtDependencies = new CbtDependencies(
      (stage2Target++".last-success").lastModified,
      mavenCache, nailgunTarget, stage1Target, stage2Target,
      new File(buildStage1.compatibilityClasspath)
    )

    logger.stage1("Compiling stage2 if necessary")
    if(loop)
      lib.addLoopFiles( cwd, stage2sourceFiles.toSet )
    val Some( stage2LastModified ) = compile(
      buildStage1.stage1LastModified,
      stage2sourceFiles, stage2Target, stage2StatusFile,
      _cbtDependencies.stage2Dependency.dependencies,
      mavenCache,
      Seq("-deprecation","-feature","-unchecked"),
      zincVersion = constants.zincVersion, scalaVersion = constants.scalaVersion
    )

    def cbtDependencies = new CbtDependencies(
      (stage2Target++".last-success").lastModified,
      mavenCache, nailgunTarget, stage1Target, stage2Target,
      new File(buildStage1.compatibilityClasspath)
    )

    logger.stage1(s"calling CbtDependency.classLoader")

    assert(
      buildStage1.compatibilityClasspath === cbtDependencies.compatibilityDependency.classpath.string,
      "compatibility classpath different from NailgunLauncher"
    )
    assert(
      buildStage1.stage1Classpath === cbtDependencies.stage1Dependency.classpath.string,
      "stage1 classpath different from NailgunLauncher"
    )
    assert(
      classLoaderCache.containsKey( cbtDependencies.compatibilityDependency.classpath.string, cbtDependencies.compatibilityDependency.lastModified ),
      "cbt unchanged, expected compatibility classloader to be cached"
    )
    assert(
      classLoaderCache.containsKey( cbtDependencies.stage1Dependency.classpath.string, cbtDependencies.stage1Dependency.lastModified ),
      "cbt unchanged, expected stage1 classloader to be cached"
    )

    val stage2ClassLoader = cbtDependencies.stage2Dependency.classLoader

    {
      // a few classloader sanity checks
      val compatibilityClassLoader =
        cbtDependencies.compatibilityDependency.classLoader
      assert(
        classOf[BuildInterface].getClassLoader == compatibilityClassLoader,
        classOf[BuildInterface].getClassLoader.toString ++ "\n\nis not the same as\n\n" ++ compatibilityClassLoader.toString
      )
      //-------------
      val stage1ClassLoader =
        cbtDependencies.stage1Dependency.classLoader
      assert(
        classOf[Stage1ArgsParser].getClassLoader == stage1ClassLoader,
        classOf[Stage1ArgsParser].getClassLoader.toString ++ "\n\nis not the same as\n\n" ++ stage1ClassLoader.toString
      )
      //-------------
      assert(
        Stage0Lib.get(stage2ClassLoader.getParent,"parents").asInstanceOf[Seq[ClassLoader]].contains(stage1ClassLoader),
        stage1ClassLoader.toString ++ "\n\nis not contained in parents of\n\n" ++ stage2ClassLoader.toString
      )
    }

    ( stage2sourceFiles, stage2LastModified, stage2ClassLoader )
  }

  def run(
    _args: Array[String],
    cache: File,
    cbtHome: File,
    loop: Boolean,
    buildStage1: BuildStage1Result,
    persistentCache: java.util.Map[AnyRef,AnyRef]
  ): Int = {
    val args = Stage1ArgsParser(_args.toVector.drop(1))
    implicit val logger = new Logger(args.enabledLoggers, buildStage1.start)
    logger.stage1(s"Stage1 start")

    implicit val transientCache: java.util.Map[AnyRef,AnyRef] = new java.util.HashMap
    implicit val classLoaderCache = new ClassLoaderCache( persistentCache )

    val cwd = new File( args.args(0) )
    val (stage2sourceFiles, stage2LastModified, classLoader) = buildStage2( buildStage1, cbtHome, cache, cwd, loop )

    val stage2Args = new Stage2Args(
      cwd,
      args.args.drop(2).toVector,
      // launcher changes cause entire nailgun restart, so no need for them here
      stage2LastModified = stage2LastModified,
      cache,
      cbtHome,
      new File(buildStage1.compatibilityClasspath),
      stage2sourceFiles,
      loop
    )

    logger.stage1(s"Run Stage2")
    val exitCode = (
      classLoader
      .loadClass(
        if(args.tools) "cbt.ToolsStage2" else "cbt.Stage2"
      )
      .getMethod( "run", classOf[Stage2Args] )
      .invoke(
        null,
        stage2Args
      ) match {
        case code: ExitCode => code
        case _ => ExitCode.Success
      }
    ).integer
    logger.stage1(s"Stage1 end with exit code " + exitCode)
    return exitCode;
  }
}
