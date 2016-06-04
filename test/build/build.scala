import cbt._
class Build(context: cbt.Context) extends BasicBuild(context){
  override def dependencies = Seq( context.cbtDependency ) ++ super.dependencies 
}
