import cbt._
import java.io.File
import scala.collection.immutable.Seq
class Build(val context: cbt.Context) extends BaseBuild{
  override def dependencies = Seq( context.cbtDependency ) ++ super.dependencies 
}
