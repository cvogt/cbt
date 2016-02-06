package cbt
object constants{
  def scalaVersion = Option(System.getenv("SCALA_VERSION")).get
}
