package cbt
object constants{
  val scalaVersion = NailgunLauncher.SCALA_VERSION
  val scalaMajorVersion = scalaVersion.split("\\.").take(2).mkString(".")
}
