package cbt

import java.io._
import java.time.LocalTime.now

import scala.collection.immutable.Seq
import scala.collection.JavaConverters._

import paths._

final case class Stage1ArgsParser(_args: Seq[String]) {
  /**
   * Raw parameters including their `-D` flag.
  **/
  val propsRaw: Seq[String] = _args.toVector.filter(_.startsWith("-D"))

  val pathSep = if (System.getProperty("os.name").contains("Windows")) "\\" else "/"

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

  def run(_args: Array[String], classLoader: ClassLoader, stage1SourcesChanged: java.lang.Boolean): Int = {
    val args = Stage1ArgsParser(_args.toVector)
    val logger = new Logger(args.enabledLoggers)
    logger.stage1(s"Stage1 start")

    val lib = new Stage1Lib(logger)
    import lib._

    val sourceFiles = stage2.listFiles.toVector.filter(_.isFile).filter(_.toString.endsWith(".scala"))
    val changeIndicator = stage2Target ++ "/cbt/Build.class"

    val deps = Dependencies(
      JavaDependency("net.incongru.watchservice","barbary-watchservice","1.0"),
      JavaDependency("org.eclipse.jgit", "org.eclipse.jgit", "4.2.0.201601211800-r")
    )

    val classLoaderCache = new ClassLoaderCache(logger)

    val stage2SourcesChanged = lib.needsUpdate(sourceFiles, stage2StatusFile)
    logger.stage1("Compiling stage2 if necessary")
    val scalaXml = JavaDependency("org.scala-lang.modules","scala-xml_"+constants.scalaMajorVersion,constants.scalaXmlVersion)
    compile(
      stage2SourcesChanged,
      sourceFiles, stage2Target, stage2StatusFile,
      nailgunTarget +: stage1Target +: Dependencies(deps, scalaXml).classpath,
      Seq("-deprecation"), classLoaderCache,
      zincVersion = "0.3.9", scalaVersion = constants.scalaVersion
    )

    logger.stage1(s"[$now] calling CbtDependency.classLoader")

    val cl = /*classLoaderCache.transient.get(
      (stage2Target +: deps.classpath).string,*/
      cbt.URLClassLoader(
        ClassPath(Seq(stage2Target)),
        classLoaderCache.persistent.get(
          deps.classpath.string,
          cbt.URLClassLoader( deps.classpath, classLoader )
        )
      )
    //)

    logger.stage1(s"[$now] Run Stage2")
    val exitCode = (
      cl.loadClass(
        if(args.admin) "cbt.AdminStage2" else "cbt.Stage2"
      )
      .getMethod( "run", classOf[Stage2Args] )
      .invoke(
        null,
        Stage2Args(
          new File( args.args(0) ),
          args.args.drop(1).toVector,
          // launcher changes cause entire nailgun restart, so no need for them here
          cbtHasChanged = stage1SourcesChanged || stage2SourcesChanged,
          logger
        )
      ) match {
        case code: ExitCode => code
        case _ => ExitCode.Success
      }
    ).integer
    logger.stage1(s"[$now] Stage1 end")
    return exitCode;
  }
}
