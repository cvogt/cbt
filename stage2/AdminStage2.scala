package cbt
import java.io._
object AdminStage2{
  def main(_args: Array[String]) = {
    val args = _args.drop(1).dropWhile(Seq("admin","direct") contains _)
    val init = new Init(args)
    val lib = new Lib(init.logger)
    val adminTasks = new AdminTasks(lib, args, new File(_args(0)))
    new lib.ReflectObject(adminTasks){
      def usage: String = "Available methods: " ++ lib.taskNames(subclassType).mkString("  ")
    }.callNullary(args.lift(0))
  }
}
