import cbt._
class Build(val context: Context) extends BaseBuild{
  override def dependencies = Seq(
    DirectoryDependency(projectDirectory++"/sub1"),
    DirectoryDependency(projectDirectory++"/sub2")
  ) ++ super.dependencies 
}
