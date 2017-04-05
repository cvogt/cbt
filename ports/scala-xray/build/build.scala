package cbt_ports.sxr
import cbt._
import java.net._
import java.io._
class Build(val context: Context) extends PackageJars with AdvancedScala{ outer =>
  override def defaultScalaVersion = "2.11.8"
  def groupId = "org.scala-sbt"

  private def gitHash =
    if( scalaVersion.startsWith("2.12") ) "cb66c7aaad618dc072d75f5899d9fdf3e8fde8d8"
    else if( scalaVersion.startsWith("2.11") ) "cb66c7aaad618dc072d75f5899d9fdf3e8fde8d8"
    else if( scalaVersion.startsWith("2.10") ) "1239fa39b5ee8c171af3f9df201497561d749826"
    else throw new Exception( "Unsupported scalaVersion: " + scalaVersion )

  private def gitUrl =
    if( scalaVersion.startsWith("2.12") ) "https://github.com/SethTisue/browse.git"
    else if( scalaVersion.startsWith("2.11") ) "https://github.com/SethTisue/browse.git"
    else if( scalaVersion.startsWith("2.10") ) "https://github.com/sbt/browse.git"
    else throw new Exception( "Unsupported scalaVersion: " + scalaVersion )

  def version = "rev-"+gitHash

  override def dependencies = Seq( libraries.scala.compiler )
  val github = GitDependency.checkout( gitUrl, gitHash )
  override def sources = Seq( github / "src" / "main" / "scala" )

  override def resourceClasspath = {
    val jquery_version = "1.3.2"
    val jquery_scrollto_version = "1.4.2"
    val jquery_qtip_version = "2.1.1"

    val resourcesManaged = github / "target" / "resources_managed"
    lib.write(
      resourcesManaged / "jquery-all.js",
      Seq(
        "https://code.jquery.com/jquery-" ~ jquery_version ~ ".min.js",
        "http://cdn.jsdelivr.net/jquery.scrollto/" ~ jquery_scrollto_version ~ "/jquery.scrollTo.min.js",
        "http://qtip2.com/v/" ~ jquery_qtip_version ~ "/jquery.qtip.min.js"
      ).map{ s =>
        val url = new URL( s )
        val file = resourcesManaged / new File( url.getPath ).getName
        lib.download( url, file, None )
        file.readAsString
      }.mkString("\n")
    )

    ClassPath(
      Seq(
        github / "src" / "main" / "resources",
        resourcesManaged
      )
    )
  }
}
