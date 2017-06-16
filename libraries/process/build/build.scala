package cbt_build.process
import cbt._
import cbt_internal._
class Build(val context: Context) extends Library{
  override def inceptionYear = 2017
  override def description = "helpers for process calls"
  override def dependencies = super.dependencies ++ Resolver(mavenCentral).bind(
    MavenDependency( "net.java.dev.jna", "jna-platform", "4.4.0" )
  ) :+ libraries.cbt.common_1
}
