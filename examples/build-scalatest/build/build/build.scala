import cbt._
import java.net.URL
import java.io.File
import scala.collection.immutable.Seq

class Build( context: Context ) extends BuildBuild( context ){

  override def dependencies = (
    super.dependencies ++
      Resolver( mavenCentral ).bind(
      ScalaDependency("org.scalatest","scalatest","2.2.4")
    ) :+ BuildDependency(new File(context.cbtHome + "/plugins/scalatest"))
  )
}
