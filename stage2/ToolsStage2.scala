package cbt
import java.io._
object ToolsStage2 extends Stage2Base{
  def run( _args: Stage2Args ): Unit = {
    val args = _args.args.dropWhile(Seq("tools","direct") contains _)
    val lib = new Lib(_args.logger)
    val toolsTasks = new ToolsTasks(lib, args, _args.cwd, _args.classLoaderCache, _args.cache, _args.cbtHome, _args.stage2LastModified)
    new lib.ReflectObject(toolsTasks){
      def usage: String = "Available methods: " ++ lib.taskNames(toolsTasks.getClass).mkString("  ")
    }.callNullary(args.lift(0))
  }
}
