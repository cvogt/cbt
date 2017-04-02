package cbt
object constants{
  val scalaXmlVersion = EarlyDependencies.scalaXmlVersion
  val scalaVersion = EarlyDependencies.scalaVersion
  val zincVersion = EarlyDependencies.zincVersion
  val scalaMajorVersion = scalaVersion.split("\\.").take(2).mkString(".")
}
