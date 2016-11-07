import java.io.{File, IOException}
import java.net.MalformedURLException

import scala.io.Source
import scala.util.{Failure, Success, Try}
import scalaj.http.Http

object Main {

  private val maven_host = "search.maven.org"
  private val cbt_home = System.getenv("CBT_HOME")

  implicit class StringExtensionMethods(str: String) {
    def /(s: String): String = str + File.separator + s
  }

  val uiPort = 9080

  def main(args: Array[String]) = launchUi(new File(args(0)), args(1))

  def launchUi(projectDirectory: File, scalaMajorVersion: String): Unit = {
    val staticBase = new File(cbt_home / "tools" / "gui" / "resources" / "web").toURI.toURL.toExternalForm
    val server = new JettyServer(uiPort, staticBase) {
      override def route(method: String, path: String, param: String => String) = (method, path) match {
        case ("GET", "/cwd") =>
          Success(s"""["$projectDirectory"]""")
        case ("POST", "/project") =>
          val name = param("name")
          val defaultPackage = param("pack")
          val dependencies = param("dependencies")
          val flags = param("flags")
          handleIoException {
            new ProjectBuilder(name, defaultPackage, dependencies, flags, projectDirectory, scalaMajorVersion).build()
            Success("[]")
          }
        case ("GET", "/dependency") =>
          val query = param("query")
          handleIoException(handleMavenBadResponse(searchDependency(query)))
        case ("GET", "/dependency/version") =>
          val group = param("group")
          val artifact = param("artifact")
          handleIoException(handleMavenBadResponse(searchDependencyVersion(group, artifact)))
        case _ =>
          Failure(new MalformedURLException(s"Incorrect path: $path"))
      }
    }
    server.start()
    java.awt.Desktop.getDesktop.browse(new java.net.URI(s"http://localhost:$uiPort/"))

    println("Press Enter to stop UI server.")
    while (Source.stdin.getLines().next().nonEmpty) {}
    server.stop()
  }

  private def searchDependency(query: String) = {
    Http(s"http://$maven_host/solrsearch/select")
        .param("q", query)
        .param("rows", "30")
        .param("wt", "json")
        .asString.body
  }

  private def searchDependencyVersion(group: String, artifact: String) = {
    val query = s"""q=g:"$group"+AND+a:"$artifact""""
    Http(s"http://$maven_host/solrsearch/select?" + query)
        .param("rows", "30")
        .param("wt", "json")
        .param("core", "gav")
        .asString.body
  }

  private def handleIoException(f: => Try[String]) = try f catch {
    case e: IOException =>
      e.printStackTrace()
      Failure(e)
  }

  private def handleMavenBadResponse(result: String) = {
    if (result.startsWith("{"))
      Success(result)
    else
      Failure(new Exception(s"Bad response from $maven_host: $result"))
  }

}
