package cbt_build.cbt.capture_args
import cbt._
import cbt_internal._
class Build(val context: Context) extends Library{
  def description = (
    "macro that allows you to extract a functions arguments"
    ++" as strings in order to programmatically pass them to a stringly typed"
    ++" api such as a process call, http or a .main method"
  )

  def inceptionYear = 2017

  override def dependencies = (
    super.dependencies ++ // don't forget super.dependencies here for scala-library, etc.
    Resolver( mavenCentral ).bind(
      MavenDependency( "org.scala-lang", "scala-reflect", scalaVersion )
    )
  )

  override def scalacOptions = super.scalacOptions :+ "-language:experimental.macros"
}
