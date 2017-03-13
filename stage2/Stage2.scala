package cbt
import java.io._
import java.util.{Set=>_,_}

object Stage2 extends Stage2Base{
  def getBuild(context: Context) = {
    new Lib( context.logger ).loadRoot( context ).finalBuild( context.cwd )
  }

  def run( args: Stage2Args ): ExitCode = {
    import args.logger
    val paths = CbtPaths(args.cbtHome,args.cache)
    import paths._
    val lib = new Lib(args.logger)
    logger.stage2(s"Stage2 start")

    val task = args.args.lift( 0 )

    import scala.collection.JavaConverters._
    val context: Context = new ContextImplementation(
      args.cwd,
      args.cwd,
      args.args.drop( 1 ).toArray,
      logger.enabledLoggers.toArray,
      logger.start,
      args.stage2LastModified,
      null,
      args.classLoaderCache.hashMap,
      args.transientCache,
      args.cache,
      args.cbtHome,
      args.cbtHome,
      args.compatibilityTarget,
      null,
      args.loop
    )
    val first = lib.loadRoot( context )
    val build = first.finalBuild( context.cwd )
    val code = lib.callReflective(build, task, context)
    logger.stage2(s"Stage2 end with exit code "+code.integer)
    code
  }
}
