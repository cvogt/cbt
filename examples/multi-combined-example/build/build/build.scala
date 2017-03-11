package cbt_build.cbt_examples.multi_combined_example.build
import cbt._
class Build(val context: Context) extends BuildBuild{
  //println(DirectoryDependency( projectDirectory / ".." / "sub4" / "build" ).exportedClasspath)
  override def dependencies: Seq[cbt.Dependency] =
    super.dependencies :+ DirectoryDependency( projectDirectory / ".." / "sub4" / "build" )
  def foo = DirectoryDependency( projectDirectory / ".." / "sub4" / "build" )
}
