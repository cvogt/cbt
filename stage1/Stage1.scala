package cbt

import java.io._
import java.util.concurrent.ConcurrentHashMap

import scala.collection.JavaConverters._

final case class Stage1ArgsParser(__args: Seq[String]) {
  val _args = __args.drop(1)
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
  def run( context: Stage2Args ): Unit
}

case class Stage2Args(
  cwd: File,
  args: Seq[String],
  cbtHasChanged: Boolean,
  classLoaderCache: ClassLoaderCache,
  cache: File,
  cbtHome: File,
  compatibilityTarget: File
){
  val ClassLoaderCache(
    logger,
    permanentKeys,
    permanentClassLoaders
  ) = classLoaderCache  
}
object Stage1{
  protected def newerThan( a: File, b: File ) ={
    a.lastModified > b.lastModified
  }

  def getBuild( _context: java.lang.Object, buildStage1: BuildStage1Result ) = {
    val context = _context.asInstanceOf[Context]
    val logger = new Logger( context.enabledLoggers, context.start )
    val (changed, classLoader) = buildStage2(
      buildStage1,
      ClassLoaderCache(
        logger,
        context.permanentKeys,
        context.permanentClassLoaders
      ),
      context.cbtHome,
      context.cache
    )

    classLoader
      .loadClass("cbt.Stage2")
      .getMethod( "getBuild", classOf[Context] )
      .invoke(
        null,
        context.copy(
          cbtHasChanged = context.cbtHasChanged || buildStage1.changed || changed // might be redundant
        )
      )
  }

  def buildStage2(
    buildStage1: BuildStage1Result, classLoaderCache: ClassLoaderCache, cbtHome: File, cache: File
  ): (Boolean, ClassLoader) = {
    import classLoaderCache.logger

    val lib = new Stage1Lib(logger)
    import lib._
    val paths = CbtPaths(cbtHome, cache)
    import paths._

    val stage2sourceFiles = (
      stage2.listFiles ++ (stage2 ++ "/plugins").listFiles
    ).toVector.filter(_.isFile).filter(_.toString.endsWith(".scala"))
    
    val cbtHasChanged = buildStage1.changed || lib.needsUpdate(stage2sourceFiles, stage2StatusFile)

    val cls = this.getClass.getClassLoader.loadClass("cbt.NailgunLauncher")
    
    val cbtDependency = CbtDependency(cbtHasChanged, mavenCache, nailgunTarget, stage1Target, stage2Target, new File(buildStage1.compatibilityClasspath))

    logger.stage1("Compiling stage2 if necessary")
    compile(
      cbtHasChanged,
      cbtHasChanged,
      stage2sourceFiles, stage2Target, stage2StatusFile,
      cbtDependency.dependencyClasspath,
      mavenCache,
      Seq("-deprecation","-feature","-unchecked"), classLoaderCache,
      zincVersion = constants.zincVersion, scalaVersion = constants.scalaVersion
    )

    logger.stage1(s"calling CbtDependency.classLoader")
    if( cbtHasChanged && classLoaderCache.persistent.containsKey( cbtDependency.classpath.string ) ) {
      classLoaderCache.persistent.remove( cbtDependency.classpath.string )
    } else {
      assert(
        buildStage1.compatibilityClasspath === cbtDependency.stage1Dependency.compatibilityDependency.classpath.string,
        "compatibility classpath different from NailgunLauncher"
      )
      assert(
        buildStage1.stage1Classpath === cbtDependency.stage1Dependency.classpath.string,
        "stage1 classpath different from NailgunLauncher"
      )
      assert(
        classLoaderCache.persistent.containsKey( cbtDependency.stage1Dependency.compatibilityDependency.classpath.string ),
        "cbt unchanged, expected compatibility classloader to be cached"
      )
      assert(
        classLoaderCache.persistent.containsKey( cbtDependency.stage1Dependency.classpath.string ),
        "cbt unchanged, expected stage1/nailgun classloader to be cached"
      )
    }

    val stage2ClassLoader = cbtDependency.classLoader(classLoaderCache)

    {
      // a few classloader sanity checks
      val compatibilityClassLoader =
        cbtDependency.stage1Dependency.compatibilityDependency.classLoader(classLoaderCache)
      assert(
        classOf[BuildInterface].getClassLoader == compatibilityClassLoader,
        classOf[BuildInterface].getClassLoader.toString ++ "\n\nis not the same as\n\n" ++ compatibilityClassLoader.toString
      )
      //-------------
      val stage1ClassLoader =
        cbtDependency.stage1Dependency.classLoader(classLoaderCache)
      assert(
        classOf[Stage1Dependency].getClassLoader == stage1ClassLoader,
        classOf[Stage1Dependency].getClassLoader.toString ++ "\n\nis not the same as\n\n" ++ stage1ClassLoader.toString
      )
      //-------------
      assert(
        Stage0Lib.get(stage2ClassLoader.getParent,"parents").asInstanceOf[Seq[ClassLoader]].contains(stage1ClassLoader),
        stage1ClassLoader.toString ++ "\n\nis not contained in parents of\n\n" ++ stage2ClassLoader.toString
      )
    }

    ( cbtHasChanged, stage2ClassLoader )
  }

  def run(
    _args: Array[String],
    cache: File,
    cbtHome: File,
    buildStage1: BuildStage1Result,
    start: java.lang.Long,
    classLoaderCacheKeys: ConcurrentHashMap[String,AnyRef],
    classLoaderCacheValues: ConcurrentHashMap[AnyRef,ClassLoader]
  ): Int = {
    val args = Stage1ArgsParser(_args.toVector)
    val logger = new Logger(args.enabledLoggers, start)
    logger.stage1(s"Stage1 start")

    val classLoaderCache = ClassLoaderCache(
      logger,
      classLoaderCacheKeys,
      classLoaderCacheValues
    )


    val (cbtHasChanged, classLoader) = buildStage2( buildStage1, classLoaderCache, cbtHome, cache )

    val stage2Args = Stage2Args(
      new File( args.args(0) ),
      args.args.drop(1).dropWhile(_ == "direct").toVector,
      // launcher changes cause entire nailgun restart, so no need for them here
      cbtHasChanged = cbtHasChanged,
      classLoaderCache = classLoaderCache,
      cache,
      cbtHome,
      new File(buildStage1.compatibilityClasspath)
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
    logger.stage1(s"Stage1 end")
    return exitCode;
  }
}
