import cbt._
import java.net.URL
import java.io.File
import scala.collection.immutable.Seq

class Build(context: Context) extends BasicBuild(context){
  override def dependencies = super.dependencies ++ Seq(
    MavenResolver(context.cbtHasChanged,context.paths.mavenCache,MavenResolver.central).resolve(
      ScalaDependency("org.scalatest","scalatest","2.2.4")
    ),
    context.cbtDependency
  )
}
