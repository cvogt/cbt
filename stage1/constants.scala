package cbt
object constants{
  val scalaVersion = Option(System.getenv("SCALA_VERSION")).get
  val scalaMajorVersion = scalaVersion.split("\\.").take(2).mkString(".")
}
