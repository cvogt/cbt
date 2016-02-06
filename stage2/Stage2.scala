package cbt
import cbt.paths._
import java.io._
import scala.collection.immutable.Seq

object Stage2{
  def main(args: Array[String]) = {
    import java.time.LocalTime.now
    val init = new Stage1.Init(args)
    import java.time._
    val start = LocalTime.now()
    def timeTaken = Duration.between(start, LocalTime.now()).toMillis
    init.logger.stage2(s"[$now] Stage2 start")

    import init._
    val loop = argsV.lift(1) == Some("loop")
    val direct = argsV.lift(1) == Some("direct")
    val taskIndex = if(loop || direct) 2 else 1
    val task = argsV.lift( taskIndex )

    val lib = new Lib(new Stage1.Init(args).logger)

    val context = Context( cwd, argsV.drop( taskIndex + 1 ), logger )
    val first = lib.loadRoot( context )
    val build = first.finalBuild

    val res = if( loop ){
      // TODO: this should allow looping over task specific files, like test files as well
      val triggerFiles = first.triggerLoopFiles.map(lib.realpath)
      val triggerCbtFiles = Seq( nailgun, stage1, stage2 ).map(lib.realpath _)
      val allTriggerFiles = triggerFiles ++ triggerCbtFiles

      logger.loop("Looping change detection over:\n - "+allTriggerFiles.mkString("\n - "))

      lib.watch(allTriggerFiles){
        case file if triggerCbtFiles.exists(file.toString startsWith _.toString) =>
          logger.loop("Change is in CBT' own source code.")
          logger.loop("Restarting CBT.")
          scala.util.control.Breaks.break
        case file if triggerFiles.exists(file.toString startsWith _.toString) =>
          new lib.ReflectBuild( lib.loadDynamic(context) ).callNullary(task)
      }
    } else new lib.ReflectBuild(build).callNullary(task)
    init.logger.stage2(s"[$now] Stage2 end")
    res
  }
}
