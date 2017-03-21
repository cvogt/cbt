package cbt_internal
import cbt._
import java.net.URL
trait Shared extends AdvancedScala with SonatypeRelease with SnapshotVersion with GithubPom{
  override def user = "cvogt"
  override def groupId = "org.cvogt"
  override def organization = Some( Organization( "Jan Christopher Vogt", Some( new URL("http://cvogt.org") ) ) )
  override def licenses = Seq( License.Apache2 )
  override def developers = Seq(cvogt)
  override def githubProject = "cbt"

  def cvogt = Developer("cvogt", "Jan Christopher Vogt", "-5", new URL("https://github.com/cvogt/"))
}
