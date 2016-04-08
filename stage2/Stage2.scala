package cbt

import java.io._

import scala.collection.immutable.Seq

import cbt.paths._

object Stage2 extends Stage2Base{
  def run( args: Stage2Args ): Unit = {
    import args.logger

    val lib = new Lib(args.logger)

    logger.stage2(s"Stage2 start")
    val loop = args.args.lift(0) == Some("loop")
    val direct = args.args.lift(0) == Some("direct")
    val cross = args.args.lift(0) == Some("cross")

    val taskIndex = if (loop || direct || cross) {
      1
    } else {
      0
    }
    val task = args.args.lift( taskIndex )
    
    val context = Context( args.cwd, args.cwd, args.args.drop( taskIndex ), logger, args.cbtHasChanged, args.classLoaderCache )
    val first = lib.loadRoot( context )
    val build = first.finalBuild

    def call(build: Build) = {
      if(cross){
        build.crossScalaVersions.foreach{
          v => new lib.ReflectBuild(
            build.copy(context.copy(scalaVersion = Some(v)))
          ).callNullary(task)
        }
      } else {
        new lib.ReflectBuild(build).callNullary(task)
      }
    }

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
            logger.loop(s"Re-running $task for " ++ build.projectDirectory.toString)
            call(build)
        }
      } else {
        call(build)
      }

    logger.stage2(s"Stage2 end")
  }
}
