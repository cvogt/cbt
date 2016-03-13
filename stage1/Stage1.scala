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

object Stage1 extends Stage1Base{
  def mainClass = ("cbt.Stage2")
}

object AdminStage1 extends Stage1Base{
  def mainClass = ("cbt.AdminStage2")
}

abstract class Stage1Base{
  def mainClass: String

  protected def newerThan( a: File, b: File ) ={
    a.lastModified > b.lastModified
  }

  def main(args: Array[String]): Unit = {
    val init = new Init(args)
    val lib = new Stage1Lib(init.logger)
    import lib._

    logger.stage1(s"[$now] Stage1 start")
    logger.stage1("Stage1: after creating lib")

    val cwd = args(0)

    val src = stage2.listFiles.toVector.filter(_.isFile).filter(_.toString.endsWith(".scala"))
    val changeIndicator = stage2Target ++ "/cbt/Build.class"

    logger.stage1("before conditionally running zinc to recompile CBT")
    if( src.exists(newerThan(_, changeIndicator)) ) {
      val stage1Classpath = CbtDependency()(logger).dependencyClasspath
      logger.stage1("cbt.lib has changed. Recompiling with cp: " ++ stage1Classpath.string)
      zinc( true, src, stage2Target, stage1Classpath, Seq("-deprecation") )( zincVersion = "0.3.9", scalaVersion = constants.scalaVersion )
    }
    logger.stage1(s"[$now] calling CbtDependency.classLoader")

    logger.stage1(s"[$now] Run Stage2")
    val ExitCode(exitCode) = /*trapExitCode*/{ // this 
      runMain( mainClass, cwd +: args.drop(1).toVector, CbtDependency()(logger).classLoader )
    }
    logger.stage1(s"[$now] Stage1 end")
    System.exit(exitCode)
  }
}
