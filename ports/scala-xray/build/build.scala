package cbt_ports.sxr
import cbt._
import java.net._
import java.io._
class Build(val context: Context) extends PackageJars with AdvancedScala with CommandLineOverrides{ outer =>
  override def defaultScalaVersion = "2.11.8"
  def groupId = "org.scala-sbt"

  private def gitHash = scalaVersion match {
    case "2.12.0" => "6484c9e90fae956044653e4dd764d8fdd15ccf99"
    case v if v.startsWith( "2.12" ) => "67261cdbcc27a1e044d7b280b2db0a02ba27add5"
    case v if v.startsWith( "2.11" ) => "cb66c7aaad618dc072d75f5899d9fdf3e8fde8d8"
    case v if v.startsWith( "2.10" ) => "1239fa39b5ee8c171af3f9df201497561d749826"
    case v => throw new Exception( "Unsupported scalaVersion: " + v )
  }

  private def gitUrl = "https://github.com/cvogt/browse.git"

  def version = "rev-"+gitHash

  override def dependencies = Seq( libraries.scala.compiler )
  lazy val github = GitDependency.checkout( gitUrl, gitHash )
  override def sources = Seq(
    github / "src" / "main" / "scala"
  )

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
