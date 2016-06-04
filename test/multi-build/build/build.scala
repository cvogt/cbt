import cbt._
class Build(context: Context) extends BasicBuild(context){
  override def dependencies = Seq(
    BuildDependency(projectDirectory++"/sub1"),
    BuildDependency(projectDirectory++"/sub2")
  ) ++ super.dependencies 
}
