package cbt
import java.io._
import java.nio.file.Files.readAllBytes
import java.net._
import org.eclipse.jgit.api._
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.lib.Ref

object GitDependency{
  val GitUrl = "(git:|https:|file:/)//([^/]+)/(.+)".r
}
case class GitDependency(
  url: String, ref: String, subDirectory: Option[String] = None // example: git://github.com/cvogt/cbt.git#<some-hash>
)(implicit val logger: Logger, classLoaderCache: ClassLoaderCache, context: Context ) extends DependencyImplementation{
  import GitDependency._
  override def lib = new Lib(logger)

  // TODO: add support for authentication via ssh and/or https
  // See http://www.codeaffine.com/2014/12/09/jgit-authentication/
  private val GitUrl( _, domain, path ) = url  

  private val credentialsFile = context.projectDirectory ++ "/git.login"

  private object checkoutCache extends Cache[File]
  def checkout: File = checkoutCache{
    val checkoutDirectory = context.cache ++ s"/git/$domain/$path/$ref"
    if(checkoutDirectory.exists){
      logger.git(s"Found existing checkout of $url#$ref in $checkoutDirectory")
    } else {
      logger.git(s"Cloning $url into $checkoutDirectory")
      val git = {
        val _git = Git
          .cloneRepository()
          .setURI(url)
          .setDirectory(checkoutDirectory)
      
        if(!credentialsFile.exists){
          _git
        } else {
          val (user, password) = {
            // TODO: implement safer method than reading credentials from plain text file
            val c = new String(readAllBytes(credentialsFile.toPath)).split("\n").head.trim.split(":")
            (c(0), c.drop(1).mkString(":"))
          }
          _git.setCredentialsProvider( new UsernamePasswordCredentialsProvider(user, password) )
        }
      }.call()

      logger.git(s"Checking out ref $ref")
      git.checkout()
         .setName(ref)
         .call()
    }
    checkoutDirectory
  }
  private object dependencyCache extends Cache[DependencyImplementation]
  def dependency = dependencyCache{
    DirectoryDependency(
      context.copy(
        projectDirectory = checkout ++ subDirectory.map("/" ++ _).getOrElse("")
      )
    )
  }

  def dependencies = Seq(dependency)

  def exportedClasspath = ClassPath()
  private[cbt] def targetClasspath = exportedClasspath
  def needsUpdate: Boolean = false
}
