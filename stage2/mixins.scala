package cbt
package mixins
import scala.collection.immutable.Seq
import java.io._
trait Test extends BasicBuild{
  lazy val testedBuild = BuildDependency( projectDirectory.parent )
  override def dependencies = Seq( testedBuild ) ++ super.dependencies
  override def defaultScalaVersion = testedBuild.build.scalaVersion
}
trait SbtTest extends Test{
  override def sources = Vector( projectDirectory.parent ++ "/src/test/scala" )
}
