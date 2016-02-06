import scala.collection.immutable.Seq
import java.io.File
class Build(context: cbt.Context) extends cbt.Build(context){
  override def dependencies = Seq( cbt.CbtDependency(context.logger) ) ++ super.dependencies 
}
