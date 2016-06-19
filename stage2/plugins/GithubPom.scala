package cbt
import java.net.URL
trait GithubPom extends Publish{
  def user: String
  def githubProject = name
  def githubUser = user
  final def githubUserProject = githubUser ++ "/" ++ githubProject
  override def url = new URL(s"http://github.com/$githubUserProject")
  override def scmUrl = s"git@github.com:$githubUserProject.git"
  override def scmConnection = s"scm:git:$scmUrl"
}
