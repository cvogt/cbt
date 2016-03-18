import cbt._
import scala.collection.immutable.Seq
import java.io.File
class Build(context: cbt.Context) extends BasicBuild(context){
  override def dependencies = Seq(
    ScalaDependency("com.typesafe.play", "play-json", "2.4.4"),
    JavaDependency("joda-time", "joda-time", "2.9.2"),
    // the below tests pom inheritance with dependencyManagement and variable substitution
    JavaDependency("org.eclipse.jgit", "org.eclipse.jgit", "4.2.0.201601211800-r")
  ) ++ super.dependencies 
}
