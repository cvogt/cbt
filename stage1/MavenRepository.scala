package cbt
import java.io._
import java.net._
case class MavenResolver(
  cbtLastModified: Long, mavenCache: File, urls: URL*
)(
  implicit logger: Logger, transientCache: java.util.Map[AnyRef,AnyRef], classLoaderCache: ClassLoaderCache
){
  def bind( dependencies: MavenDependency* ): Seq[BoundMavenDependency]
    = dependencies.map( BoundMavenDependency(cbtLastModified,mavenCache,_,urls.toVector) ).toVector
  def bindOne( dependency: MavenDependency ): BoundMavenDependency
    = BoundMavenDependency( cbtLastModified, mavenCache, dependency, urls.toVector )
}
