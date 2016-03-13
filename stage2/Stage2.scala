package cbt

import java.io._
import java.time._
import java.time.LocalTime.now

import scala.collection.immutable.Seq

import cbt.paths._


object Stage2{
  def main(args: Array[String]): Unit = {
    val init = new Init(args)
    import init._

    val lib = new Lib(init.logger)

    init.logger.stage2(s"[$now] Stage2 start")
    val loop = argsV.lift(1) == Some("loop")
    val direct = argsV.lift(1) == Some("direct")

    val taskIndex = if (loop || direct) {
      2
    } else {
      1
    }
    val task = argsV.lift( taskIndex )

    val context = Context( new File(argsV(0)), argsV.drop( taskIndex + 1 ), logger )
    val first = lib.loadRoot( context )
    val build = first.finalBuild

    val res =
      if (loop) {
        // TODO: this should allow looping over task specific files, like test files as well
        val triggerFiles = first.triggerLoopFiles.map(lib.realpath)
        val triggerCbtFiles = Seq( nailgun, stage1, stage2 ).map(lib.realpath _)
        val allTriggerFiles = triggerFiles ++ triggerCbtFiles

        logger.loop("Looping change detection over:\n - "++allTriggerFiles.mkString("\n - "))

        lib.watch(allTriggerFiles){
          case file if triggerCbtFiles.exists(file.toString startsWith _.toString) =>
            logger.loop("Change is in CBT's own source code.")
            logger.loop("Restarting CBT.")
            scala.util.control.Breaks.break

          case file if triggerFiles.exists(file.toString startsWith _.toString) =>
            val reflectBuild = new lib.ReflectBuild( lib.loadDynamic(context) )
            logger.loop(s"Re-running $task for " ++ reflectBuild.build.projectDirectory.toString)
            reflectBuild.callNullary(task)
        }
      } else {
        new lib.ReflectBuild(build).callNullary(task)
      }

    init.logger.stage2(s"[$now] Stage2 end")
  }
}
