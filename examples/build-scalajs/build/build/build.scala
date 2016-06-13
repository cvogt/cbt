import cbt._
import java.net.URL
import java.io.File
import scala.collection.immutable.Seq

class Build( context: Context ) extends BuildBuild( context ){

  override def dependencies =
    super.dependencies :+
      BuildDependency(new File(context.cbtHome + "/plugins/scalajs"))
}
