import cbt._
class Build(val context: cbt.Context) extends BaseBuild{
  override def dependencies = Seq( context.cbtDependency ) ++ super.dependencies 
}
