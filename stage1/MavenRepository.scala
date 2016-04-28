package cbt
import scala.collection.immutable.Seq
import java.io._
import java.net._
case class MavenResolver( cbtHasChanged: Boolean, mavenCache: File, urls: URL* ){
  def resolve( dependencies: MavenDependency* )(implicit logger: Logger): BoundMavenDependencies
    = new BoundMavenDependencies( cbtHasChanged, mavenCache, urls.to, dependencies.to )
  def resolveOne( dependency: MavenDependency )(implicit logger: Logger): BoundMavenDependency
    = BoundMavenDependency( cbtHasChanged, mavenCache, dependency, urls.to )
}

object MavenResolver{
  def central = new URL("https://repo1.maven.org/maven2")
  def jcenter = new URL("https://jcenter.bintray.com/releases")
  def bintray(owner: String) = new URL(s"https://dl.bintray.com/$owner/maven")
  private val sonatypeBase  = new URL("https://oss.sonatype.org/content/repositories/")
  def sonatype          = sonatypeBase ++ "releases"
  def sonatypeSnapshots = sonatypeBase ++ "snapshots"
}
