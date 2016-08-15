import cbt._
import java.nio._
import java.nio.file._

class Build(val context: Context) extends Publish{
  // FIXME: somehow consolidate this with cbt's own boot-strapping from source.
  override def dependencies = {
    super.dependencies ++ Resolver(mavenCentral).bind(
      MavenDependency("net.incongru.watchservice","barbary-watchservice","1.0"),
      MavenDependency("org.eclipse.jgit", "org.eclipse.jgit", "4.2.0.201601211800-r"),
      MavenDependency("com.typesafe.zinc","zinc","0.3.9"),
      ScalaDependency("org.scala-lang.modules","scala-xml","1.0.5")
    )
  }
  override def sources = Seq(
    "nailgun_launcher", "stage1", "stage2", "compatibility"
  ).map(d => Paths.get( projectDirectory.toString + ("/" + d) ) )

  def groupId: String = "org.cvogt"

  def defaultVersion: String = "0.1"
  def name: String = "cbt"

  // Members declared in cbt.Publish
  def description: String = "Fast, intuitive Build Tool for Scala"
  def developers: Seq[cbt.Developer] = Nil
  def inceptionYear: Int = 2016
  def licenses: Seq[cbt.License] = Seq( License.Apache2 )
  def organization: Option[cbt.Organization] = None
  def scmConnection: String = ""
  def scmUrl: String = ""
  def url: java.net.URL = new java.net.URL("http://github.com/cvogt/cbt/")
}
