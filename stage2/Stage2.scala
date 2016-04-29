package cbt

import java.io._

import scala.collection.immutable.Seq


object Stage2 extends Stage2Base{
  def getBuild(__context: java.lang.Object, _cbtChanged: java.lang.Boolean) = {
    val _context = __context.asInstanceOf[Context]
    val context = _context.copy(
      cbtHasChanged = _context.cbtHasChanged || _cbtChanged
    )
    val first = new Lib(context.logger).loadRoot( context )
    first.finalBuild
  }

  def run( args: Stage2Args ): Unit = {
    import args.logger
    val paths = CbtPaths(args.cbtHome,args.cache)
    import paths._
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
    
    val context: Context = ContextImplementation(
      args.cwd,
      args.cwd,
      args.args.drop( taskIndex ).toArray,
      logger.enabledLoggers.toArray,
      logger.start,
      args.cbtHasChanged,
      null,
      null,
      args.permanentKeys,
      args.permanentClassLoaders,
      args.cache,
      args.cbtHome,
      args.compatibilityTarget,
      null
    )
    val first = lib.loadRoot( context )
    val build = first.finalBuild

    def call(build: BuildInterface) = {
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
            logger.loop(s"Re-running $task for " ++ build.show)
            call(build)
        }
      } else {
        call(build)
      }

    logger.stage2(s"Stage2 end")
  }
}
