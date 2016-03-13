package cbt
import scala.collection.immutable.Seq
class AdminTasks(lib: Lib, args: Array[String]){
  implicit val logger: Logger = lib.logger
  def resolve = {
    ClassPath.flatten(
      args(1).split(",").toVector.map{
        d =>
          val v = d.split(":")
          new JavaDependency(v(0),v(1),v(2))(lib.logger).classpath
      }
    )
  }
  def amm = ammonite
  def ammonite = {
    val version = args.lift(1).getOrElse(constants.scalaVersion)
    val scalac = new ScalaCompilerDependency( version )
    val d = JavaDependency(
      "com.lihaoyi","ammonite-repl_2.11.7",args.lift(1).getOrElse("0.5.6")
    )
    // FIXME: this does not work quite yet, throws NoSuchFileException: /ammonite/repl/frontend/ReplBridge$.class
    lib.runMain(
      "ammonite.repl.Main", Seq(), d.classLoader
    )
  }
  def scala = {
    val version = args.lift(1).getOrElse(constants.scalaVersion)
    val scalac = new ScalaCompilerDependency( version )
    lib.runMain(
      "scala.tools.nsc.MainGenericRunner", Seq("-cp", scalac.classpath.string), scalac.classLoader
    )
  }
}
