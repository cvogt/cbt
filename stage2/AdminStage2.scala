package cbt
import java.io._
object AdminStage2 extends Stage2Base{
  def run( _args: Stage2Args ): Unit = {
    val args = _args.args.dropWhile(Seq("admin","direct") contains _)
    val lib = new Lib(_args.logger)
    val adminTasks = new AdminTasks(lib, args, _args.cwd, _args.classLoaderCache, _args.cache, _args.cbtHome, _args.cbtHasChanged)
    new lib.ReflectObject(adminTasks){
      def usage: String = "Available methods: " ++ lib.taskNames(adminTasks.getClass).mkString("  ")
    }.callNullary(args.lift(0))
  }
}
