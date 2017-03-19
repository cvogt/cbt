package scalafix_build

import cbt._

class Build(val context: Context) extends Plugin {
  override def dependencies = (
    super.dependencies ++
    Resolver( mavenCentral, sonatypeReleases ).bind(
      ScalaDependency( "ch.epfl.scala", "scalafix-nsc",  "0.3.1" )
    )
  )
}
