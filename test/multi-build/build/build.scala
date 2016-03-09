import cbt._
import scala.collection.immutable.Seq
import java.io.File
class Build(context: Context) extends BasicBuild(context){
  override def dependencies = Seq(
    BuildDependency(projectDirectory++"/sub1"),
    BuildDependency(projectDirectory++"/sub2")
  ) ++ super.dependencies 
}
