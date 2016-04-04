package cbt
import java.io._
import java.net._
import scala.collection.immutable.Seq
import org.eclipse.jgit.api._
import org.eclipse.jgit.lib.Ref

case class GitDependency(
  url: String, ref: String // example: git://github.com/cvogt/cbt.git#<some-hash>
)(implicit val logger: Logger, classLoaderCache: ClassLoaderCache, context: Context ) extends Dependency{
  override def lib = new Lib(logger)

  override def canBeCached = true
  // TODO: add support for authentication via ssh and/or https
  // See http://www.codeaffine.com/2014/12/09/jgit-authentication/

  private val GitUrl = "(git|https)://([^/]+)/(.+)".r
  private val GitUrl( _, domain, path ) = url
  
  private object dependenciesCache extends Cache[Seq[Dependency]]
  def dependencies = dependenciesCache{
    val checkoutDirectory = paths.cbtHome ++ s"/cache/git/$domain/$path/$ref"
    if(checkoutDirectory.exists){
      logger.git(s"Found existing checkout of $url#$ref in $checkoutDirectory")
    } else {

      logger.git(s"Cloning $url into $checkoutDirectory")
      val git =
        Git.cloneRepository()
          .setURI(url)
          .setDirectory(checkoutDirectory)
          .call()
      
      logger.git(s"Checking out ref $ref")
      git.checkout()
          .setName(ref)
          .call()

    }
    val managedBuild = lib.loadDynamic(
      context.copy( cwd = checkoutDirectory, args = Seq() )
    )    
    Seq( managedBuild )
  }

  def exportedClasspath = ClassPath(Seq())
  def exportedJars = Seq()
  private[cbt] def targetClasspath = exportedClasspath
  def needsUpdate: Boolean = false
}
