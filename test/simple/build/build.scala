import cbt._
import scala.collection.immutable.Seq
import java.io.File
class Build(context: cbt.Context) extends BasicBuild(context){
  override def dependencies = Seq(
    ScalaDependency("com.typesafe.play", "play-json", "2.4.4"),
    JavaDependency("joda-time", "joda-time", "2.9.2")
  ) ++ super.dependencies 
}
