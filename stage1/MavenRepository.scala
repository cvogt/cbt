package cbt
import java.io._
import java.net._
case class MavenResolver( cbtHasChanged: Boolean, mavenCache: File, urls: URL* ){
  def bind( dependencies: MavenDependency* )(implicit logger: Logger): Seq[BoundMavenDependency]
    = dependencies.map( BoundMavenDependency(cbtHasChanged,mavenCache,_,urls.to) ).to
  def bindOne( dependency: MavenDependency )(implicit logger: Logger): BoundMavenDependency
    = BoundMavenDependency( cbtHasChanged, mavenCache, dependency, urls.to )
}
