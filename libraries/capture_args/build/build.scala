package cbt_build.cbt.capture_args
import cbt._
class Build(val context: Context) extends BaseBuild{
  override def dependencies = (
    super.dependencies ++ // don't forget super.dependencies here for scala-library, etc.
    Resolver( mavenCentral ).bind(
      MavenDependency( "org.scala-lang", "scala-reflect", scalaVersion )
    )
  )
  override def scalacOptions = super.scalacOptions :+ "-language:experimental.macros"
}
