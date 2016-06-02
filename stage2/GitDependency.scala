package cbt
import java.io._
import java.net._
import scala.collection.immutable.Seq
import org.eclipse.jgit.api._
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.lib.Ref

object GitDependency{
  val GitUrl = "(git:|https:|file:/)//([^/]+)/(.+)".r
}
case class GitDependency(
  url: String, ref: String // example: git://github.com/cvogt/cbt.git#<some-hash>
)(implicit val logger: Logger, classLoaderCache: ClassLoaderCache, context: Context ) extends DependencyImplementation{
  import GitDependency._
  override def lib = new Lib(logger)

  // TODO: add support for authentication via ssh and/or https
  // See http://www.codeaffine.com/2014/12/09/jgit-authentication/
  private val GitUrl( _, domain, path ) = url  


  def checkout: File = {
    def mkCredentials(creds: Option[Array[String]]): UsernamePasswordCredentialsProvider = creds match {
      case Some(credential) => {
        val username = credential(0)
        val password = credential(1)
        new UsernamePasswordCredentialsProvider(username, password)
      }
      case None => new UsernamePasswordCredentialsProvider("dummy", "dummy")
    }
    val checkoutDirectory = context.cache ++ s"/git/$domain/$path/$ref"
    if(checkoutDirectory.exists){
      logger.git(s"Found existing checkout of $url#$ref in $checkoutDirectory")
    } else {
      logger.git(s"Cloning $url into $checkoutDirectory")
      val authList = {
        try {
          val userNameAndPassword = scala.io.Source.fromFile(context.projectDirectory + "/git.login").mkString.split("\n")
          Some(userNameAndPassword)
        } catch {
          case e: FileNotFoundException => None
        }
      }
      val git =
        Git.cloneRepository()
          .setURI(url)
          .setCredentialsProvider( mkCredentials( authList ) )
          .setDirectory(checkoutDirectory)
          .call()

      logger.git(s"Checking out ref $ref")
      git.checkout()
          .setName(ref)
          .call()
    }
    checkoutDirectory
  }
  private object dependencyCache extends Cache[Dependency]
  def dependency = dependencyCache{
    BuildDependency( context.copy( projectDirectory = checkout ) )
  }

  def dependencies = Seq(dependency)

  def exportedClasspath = ClassPath(Seq())
  private[cbt] def targetClasspath = exportedClasspath
  def needsUpdate: Boolean = false
}
