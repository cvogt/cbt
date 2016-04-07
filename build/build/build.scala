import cbt._
import java.net.URL
import java.io.File
import scala.collection.immutable.Seq

class Build(context: Context) extends BuildBuild(context){
  override def dependencies =
    super.dependencies :+
    MavenRepository.combine(
      MavenRepository.bintray("tpolecat"),
      MavenRepository.central
    ).resolve(
      ScalaDependency("org.tpolecat", "tut-core", "0.4.2")
    )
}
