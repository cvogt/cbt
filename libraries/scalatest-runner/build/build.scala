package cbt_libraries_build.scalatest_runner
import cbt._
import cbt_internal._

class Build(val context: Context) extends Library{
  override def inceptionYear = 2017
  override def description = "run scalatest tests from given directory and classpath (compatible with 2.12 and 2.11)"

  override def dependencies = super.dependencies ++
    Resolver( mavenCentral ).bind(
      ScalaDependency("org.scalatest","scalatest", if(scalaMajorVersion == "2.12") "3.0.1" else "2.2.6")
    )
}
