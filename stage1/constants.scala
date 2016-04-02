package cbt
object constants{
  val scalaXmlVersion = NailgunLauncher.SCALA_XML_VERSION
  val scalaVersion = NailgunLauncher.SCALA_VERSION
  val zincVersion = NailgunLauncher.ZINC_VERSION
  val scalaMajorVersion = scalaVersion.split("\\.").take(2).mkString(".")
}
