package cbt

import java.io._
import java.time._
import java.time.LocalTime.now

import scala.collection.immutable.Seq

import cbt.paths._

object Stage2 extends Stage2Base{
  def run( args: Stage2Args ): Unit = {
    import args.logger

    val lib = new Lib(args.logger)

    logger.stage2(s"[$now] Stage2 start")
    val loop = args.args.lift(0) == Some("loop")
    val direct = args.args.lift(0) == Some("direct")

    val taskIndex = if (loop || direct) {
      1
    } else {
      0
    }
    val task = args.args.lift( taskIndex )

    val context = Context( args.cwd, args.args.drop( taskIndex ), logger, args.cbtHasChanged, new ClassLoaderCache(logger) )
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
            val build = lib.loadDynamic(context)
            val reflectBuild = new lib.ReflectBuild( build )
            logger.loop(s"Re-running $task for " ++ build.projectDirectory.toString)
            reflectBuild.callNullary(task)
        }
      } else {
        new lib.ReflectBuild(build).callNullary(task)
      }

    logger.stage2(s"[$now] Stage2 end")
  }
}
