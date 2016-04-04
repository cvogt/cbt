import cbt._
import java.net.URL
import java.io.File
import scala.collection.immutable.Seq

class Build(context: Context) extends BasicBuild(context){
  // FIXME: somehow consolidate this with cbt's own boot-strapping from source.
  override def dependencies = super.dependencies :+ MavenRepository.central.resolve(
    MavenDependency("net.incongru.watchservice","barbary-watchservice","1.0"),
    MavenDependency("org.eclipse.jgit", "org.eclipse.jgit", "4.2.0.201601211800-r"),
    MavenDependency("com.typesafe.zinc","zinc","0.3.9"),
    ScalaDependency("org.scala-lang.modules","scala-xml","1.0.5")
  )
  override def sources = Seq(
    "nailgun_launcher", "stage1", "stage2"
  ).map(d => projectDirectory ++ ("/" + d))
}
