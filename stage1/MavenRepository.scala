package cbt
import java.io._
import java.net._
case class MavenResolver(
  cbtHasChanged: Boolean, mavenCache: File, urls: URL*
)(
  implicit logger: Logger, transientCache: java.util.Map[AnyRef,AnyRef]
){
  def bind( dependencies: MavenDependency* ): Seq[BoundMavenDependency]
    = dependencies.map( BoundMavenDependency(cbtHasChanged,mavenCache,_,urls.to) ).to
  def bindOne( dependency: MavenDependency ): BoundMavenDependency
    = BoundMavenDependency( cbtHasChanged, mavenCache, dependency, urls.to )
}
