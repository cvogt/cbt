package cbt
import scala.collection.immutable.Seq
import java.net._
case class MavenRepository(url: URL){
    def resolve( dependencies: MavenDependency* )(implicit logger: Logger): BoundMavenDependencies
      = new BoundMavenDependencies( Seq(url), dependencies.to )
    def resolveOne( dependency: MavenDependency )(implicit logger: Logger): BoundMavenDependency
      = BoundMavenDependency( dependency, Seq(url) )
}

object MavenRepository{
  case class combine(repositories: MavenRepository*){
    def resolve( dependencies: MavenDependency* )(implicit logger: Logger): BoundMavenDependencies
      = new BoundMavenDependencies( repositories.map(_.url).to, dependencies.to )
  }
  def central = MavenRepository(new URL(NailgunLauncher.MAVEN_URL))
  def jcenter = MavenRepository(new URL("https://jcenter.bintray.com/releases"))
  def bintray(owner: String) = MavenRepository(new URL(s"https://dl.bintray.com/$owner/maven"))
  private val sonatypeBase  = new URL("https://oss.sonatype.org/content/repositories/")
  def sonatype          = MavenRepository(sonatypeBase ++ "releases")
  def sonatypeSnapshots = MavenRepository(sonatypeBase ++ "snapshots")
}
