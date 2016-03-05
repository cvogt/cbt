package cbt
import java.io._
import paths._
import scala.collection.immutable.Seq

object CheckAlive{
  def main(args: Array[String]): Unit = {
    System.exit(33)
  }
}
object Stage1 extends Stage1Base{
  def mainClass = ("cbt.Stage2")
}
object AdminStage1 extends Stage1Base{
  def mainClass = ("cbt.AdminStage2")
}
abstract class Stage1Base{
  class Init(args: Array[String]){
    import scala.collection.JavaConverters._
    val propsRaw: Seq[String] = args.toVector.filter(_.startsWith("-D"))
    val argsV: Seq[String] = args.toVector diff propsRaw

    lazy val props = propsRaw.map(_.drop(2)).map(_.split("=")).map{
      case Array(key, value) => key -> value
    }.toMap ++ System.getProperties.asScala
    val logger = new Logger(props.get("log"))

    val cwd = argsV(0)    
  }
  def mainClass: String
  def main(args: Array[String]): Unit = {
    import java.time.LocalTime.now
    val init = new Init(args)
    val lib = new Stage1Lib(init.logger)
    lib.logger.stage1(s"[$now] Stage1 start")
    lib.logger.stage1("Stage1: after creating lib")
    import lib._
    val cwd = args(0)

    val src  = stage2.listFiles.toVector.filter(_.isFile).filter(_.toString.endsWith(".scala"))

    val changeIndicator = new File(stage2Target+"/cbt/Build.class")

    def newerThan( a: File, b: File ) ={
      val res =  a.lastModified > b.lastModified
      if(res) {
        /*
        println(a)
        println(a.lastModified)
        println(b)
        println(b.lastModified)
        */
      }
      res
    }

    logger.stage1("before conditionally running zinc to recompile CBT")
    if( src.exists(newerThan(_, changeIndicator)) ){
      val stage1Classpath = CbtDependency(init.logger).dependencyClasspath
      logger.stage1("cbt.lib has changed. Recompiling with cp: "+stage1Classpath)
      lib.zinc( true, src, stage2Target, stage1Classpath )( zincVersion = "0.3.9", scalaVersion = constants.scalaVersion )
    }
    logger.stage1(s"[$now] calling CbtDependency.classLoader")

    logger.stage1(s"[$now] Run Stage2")
    lib.runMain( mainClass, cwd +: args.drop(1).toVector, CbtDependency(init.logger).classLoader )
    lib.logger.stage1(s"[$now] Stage1 end")
    
    
  } 
}
