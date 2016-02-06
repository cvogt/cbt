package cbt
object AdminStage2{
  def main(args: Array[String]) = {
    val init = new Stage1.Init(args.drop(3))
    val lib = new Lib(init.logger)
    val adminTasks = new AdminTasks(lib, args.drop(3))
    new lib.ReflectObject(adminTasks){
      def usage = "Available methods: " + lib.taskNames(subclassType)
    }.callNullary(args.lift(2))
  }
}
