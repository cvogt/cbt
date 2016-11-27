package cbt
import java.io._
import java.util._

object Stage2 extends Stage2Base{
  def getBuild(context: Context) = {
    new Lib( context.logger ).loadRoot( context ).finalBuild
  }

  def run( args: Stage2Args ): Unit = {
    import args.logger
    val paths = CbtPaths(args.cbtHome,args.cache)
    import paths._
    val lib = new Lib(args.logger)

    logger.stage2(s"Stage2 start")
    val loop = args.args.lift(0) == Some("loop")
    val cross = args.args.lift(0) == Some("cross")

    val taskIndex = if (loop || cross) {
      1
    } else {
      0
    }
    val task = args.args.lift( taskIndex )

    val context: Context = ContextImplementation(
      args.cwd,
      args.cwd,
      args.args.drop( taskIndex +1 ).toArray,
      logger.enabledLoggers.toArray,
      logger.start,
      args.cbtHasChanged,
      null,
      args.persistentCache,
      new HashMap,
      args.cache,
      args.cbtHome,
      args.cbtHome,
      args.compatibilityTarget,
      null
    )
    val first = lib.loadRoot( context )
    val build = first.finalBuild

    def call(build: BuildInterface): ExitCode = {
      if(cross){
        build.crossScalaVersions.map{
          v => new lib.ReflectBuild(
            build.copy(context.copy(scalaVersion = Some(v)))
          ).callNullary(task)
        }.filter(_ != ExitCode.Success).headOption getOrElse ExitCode.Success
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
            val build = lib.loadRoot(context).finalBuild
            logger.loop(s"Re-running $task for " ++ build.show)
            call(build)
        }
      } else {
        val code = call(build)
        logger.stage2(s"Stage2 end")
        System.exit(code.integer)
      }
  }
}
