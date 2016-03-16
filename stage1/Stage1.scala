package cbt

import java.io._
import java.time.LocalTime.now

import scala.collection.immutable.Seq
import scala.collection.JavaConverters._

import paths._

class Init(args: Array[String]) {
  /**
   * Raw parameters including their `-D` flag.
  **/
  val propsRaw: Seq[String] = args.toVector.filter(_.startsWith("-D"))

  /**
   * All arguments that weren't `-D` property declarations.
  **/
  val argsV: Seq[String] = args.toVector diff propsRaw

  /**
   * Parsed properties, as a map of keys to values.
  **/
  lazy val props = propsRaw
    .map(_.drop(2).split("=")).map({
      case Array(key, value) =>
        key -> value
    }).toMap ++ System.getProperties.asScala

  val logger = new Logger(props.get("log"))
}

object Stage1{

  protected def newerThan( a: File, b: File ) ={
    a.lastModified > b.lastModified
  }

  def main(args: Array[String], classLoader: ClassLoader): Unit = {
    val mainClass = if(args contains "admin") "cbt.AdminStage2" else "cbt.Stage2"
    val init = new Init(args)
    val lib = new Stage1Lib(init.logger)
    import lib._

    logger.stage1(s"[$now] Stage1 start")
    logger.stage1("Stage1: after creating lib")

    val cwd = args(0)

    val src = stage2.listFiles.toVector.filter(_.isFile).filter(_.toString.endsWith(".scala"))
    val changeIndicator = stage2Target ++ "/cbt/Build.class"
    
    val classLoaderCache = new ClassLoaderCache(logger)

    val deps = ClassPath.flatten(
      Seq(
        JavaDependency("net.incongru.watchservice","barbary-watchservice","1.0"),
        JavaDependency("org.scala-lang","scala-reflect",constants.scalaVersion),
        ScalaDependency(
          "org.scala-lang.modules", "scala-xml", "1.0.5", scalaVersion=constants.scalaMajorVersion
        ),
        JavaDependency("org.eclipse.jgit", "org.eclipse.jgit", "4.2.0.201601211800-r")
      ).map(_.classpath)
    )

    logger.stage1("before conditionally running zinc to recompile CBT")
    if( src.exists(newerThan(_, changeIndicator)) ) {
      logger.stage1("cbt.lib has changed. Recompiling.")
      zinc( true, src, stage2Target, nailgunTarget +: stage1Target +: deps, classLoaderCache, Seq("-deprecation") )( zincVersion = "0.3.9", scalaVersion = constants.scalaVersion )
    }
    logger.stage1(s"[$now] calling CbtDependency.classLoader")

    val cp = stage2Target
    val cl = classLoaderCache.transient.get(
      (stage2Target +: deps).string,
      cbt.URLClassLoader(
        ClassPath(Seq(stage2Target)),
        classLoaderCache.persistent.get(
          deps.string,
          cbt.URLClassLoader( deps, classLoader )
        )
      )
    )

    logger.stage1(s"[$now] Run Stage2")
    val ExitCode(exitCode) = /*trapExitCode*/{ // this 
      runMain( mainClass, cwd +: args.drop(1).toVector, cl )
    }
    logger.stage1(s"[$now] Stage1 end")
    System.exit(exitCode)
  }
}
