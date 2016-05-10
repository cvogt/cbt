import cbt._
import java.io.File
import scala.collection.immutable.Seq
class Build(context: cbt.Context) extends BasicBuild(context){
  override def dependencies = Seq( context.cbtDependency ) ++ super.dependencies 
}
