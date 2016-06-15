import cbt._
import scala.collection.immutable.Seq
import java.io.File
class Build(val context: Context) extends BaseBuild{
  override def dependencies = Seq(
    BuildDependency(projectDirectory++"/sub1"),
    BuildDependency(projectDirectory++"/sub2")
  ) ++ super.dependencies 
}
