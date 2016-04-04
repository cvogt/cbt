package cbt

import java.io._

import scala.collection.immutable.Seq
import scala.collection.JavaConverters._

import paths._

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

  val admin = _args contains "admin"
}


abstract class Stage2Base{
  def run( context: Stage2Args ): Unit
}

case class Stage2Args(
  cwd: File,
  args: Seq[String],
  cbtHasChanged: Boolean,
  logger: Logger
)

object Stage1{
  protected def newerThan( a: File, b: File ) ={
    a.lastModified > b.lastModified
  }

  def run(_args: Array[String], classLoader: ClassLoader, _cbtChanged: java.lang.Boolean, start: java.lang.Long): Int = {
    val args = Stage1ArgsParser(_args.toVector)
    val logger = new Logger(args.enabledLoggers, start)
    logger.stage1(s"Stage1 start")

    val lib = new Stage1Lib(logger)
    import lib._
    val classLoaderCache = new ClassLoaderCache(logger)

    val sourceFiles = stage2.listFiles.toVector.filter(_.isFile).filter(_.toString.endsWith(".scala"))
    val cbtHasChanged = _cbtChanged || lib.needsUpdate(sourceFiles, stage2StatusFile)
    logger.stage1("Compiling stage2 if necessary")
    compile(
      cbtHasChanged,
      sourceFiles, stage2Target, stage2StatusFile,
      CbtDependency().dependencyClasspath,
      Seq("-deprecation"), classLoaderCache,
      zincVersion = "0.3.9", scalaVersion = constants.scalaVersion
    )

    logger.stage1(s"calling CbtDependency.classLoader")
    if(cbtHasChanged){
      NailgunLauncher.stage2classLoader = CbtDependency().classLoader(classLoaderCache)
    }

    logger.stage1(s"Run Stage2")
    val exitCode = (
      NailgunLauncher.stage2classLoader.loadClass(
        if(args.admin) "cbt.AdminStage2" else "cbt.Stage2"
      )
      .getMethod( "run", classOf[Stage2Args] )
      .invoke(
        null,
        Stage2Args(
          new File( args.args(0) ),
          args.args.drop(1).toVector,
          // launcher changes cause entire nailgun restart, so no need for them here
          cbtHasChanged = cbtHasChanged,
          logger
        )
      ) match {
        case code: ExitCode => code
        case _ => ExitCode.Success
      }
    ).integer
    logger.stage1(s"Stage1 end")
    return exitCode;
  }
}
