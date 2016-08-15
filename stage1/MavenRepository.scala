package cbt
import java.nio._
import java.nio.file._
import java.net._
case class MavenResolver( cbtHasChanged: Boolean, mavenCache: Path, urls: URL* ){
  def bind( dependencies: MavenDependency* )(implicit logger: Logger): Seq[BoundMavenDependency]
    = dependencies.map( BoundMavenDependency(cbtHasChanged,mavenCache,_,urls.to) ).to
  def bindOne( dependency: MavenDependency )(implicit logger: Logger): BoundMavenDependency
    = BoundMavenDependency( cbtHasChanged, mavenCache, dependency, urls.to )
}
