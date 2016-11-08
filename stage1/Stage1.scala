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

  def getBuild( _context: java.lang.Object, _cbtChanged: java.lang.Boolean ) = {
    val context = _context.asInstanceOf[Context]
    val logger = new Logger( context.enabledLoggers, context.start )
    val (changed, classLoader) = buildStage2(
      context.compatibilityTarget,
      ClassLoaderCache(
        logger,
        context.permanentKeys,
        context.permanentClassLoaders
      ),
      _cbtChanged,
      context.cbtHome,
      context.cache
    )

    classLoader
      .loadClass("cbt.Stage2")
      .getMethod( "getBuild", classOf[java.lang.Object], classOf[java.lang.Boolean] )
      .invoke(null, context, (_cbtChanged || changed): java.lang.Boolean)
  }

  def buildStage2(
    compatibilityTarget: File, classLoaderCache: ClassLoaderCache, _cbtChanged: Boolean, cbtHome: File, cache: File
  ): (Boolean, ClassLoader) = {
    import classLoaderCache.logger

    val lib = new Stage1Lib(logger)
    import lib._
    val paths = CbtPaths(cbtHome, cache)
    import paths._

    val stage2sourceFiles = (
      stage2.listFiles ++ (stage2 ++ "/plugins").listFiles
    ).toVector.filter(_.isFile).filter(_.toString.endsWith(".scala"))
    
    val cbtHasChanged = _cbtChanged || lib.needsUpdate(stage2sourceFiles, stage2StatusFile)

    val cls = this.getClass.getClassLoader.loadClass("cbt.NailgunLauncher")
    
    val cbtDependency = CbtDependency(cbtHasChanged, mavenCache, nailgunTarget, stage1Target, stage2Target, compatibilityTarget)

    logger.stage1("Compiling stage2 if necessary")
    compile(
      cbtHasChanged,
      cbtHasChanged,
      stage2sourceFiles, stage2Target, stage2StatusFile,
      cbtDependency.dependencyClasspath,
      mavenCache,
      Seq("-deprecation","-feature","-unchecked"), classLoaderCache,
      zincVersion = "0.3.9", scalaVersion = constants.scalaVersion
    )

    logger.stage1(s"calling CbtDependency.classLoader")
    if( cbtHasChanged && classLoaderCache.persistent.containsKey( cbtDependency.classpath.string ) ) {
      classLoaderCache.persistent.remove( cbtDependency.classpath.string )
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
    _cbtChanged: java.lang.Boolean,
    compatibilityTarget: File,
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
    

    val (cbtHasChanged, classLoader) = buildStage2( compatibilityTarget, classLoaderCache, _cbtChanged, cbtHome, cache )

    val stage2Args = Stage2Args(
      new File( args.args(0) ),
      args.args.drop(1).dropWhile(_ == "direct").toVector,
      // launcher changes cause entire nailgun restart, so no need for them here
      cbtHasChanged = cbtHasChanged,
      classLoaderCache = classLoaderCache,
      cache,
      cbtHome,
      compatibilityTarget
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
