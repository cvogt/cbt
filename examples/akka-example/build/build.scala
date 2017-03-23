package cbt_examples_build.akka_example

import cbt._
import java.net.URL

class Build(val context: Context) extends BaseBuild {

  override def defaultScalaVersion = "2.12.1"

  override def dependencies =
    super.dependencies ++
    Resolver(mavenCentral).bind(
      ScalaDependency("com.typesafe.akka", "akka-http", "10.0.5")
    )

}
