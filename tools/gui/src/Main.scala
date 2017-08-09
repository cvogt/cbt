import java.io.{File, IOException}
import java.net.MalformedURLException
import java.nio.file.Files

import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

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
      override def route(method: String,
                         path: String,
                         param: String => String,
                         setContentType: String => Unit) = (method, path) match {
        case ("GET", "/cwd") =>
          Success(s"""["$projectDirectory"]""")
        case ("POST", "/project/new") =>
          val name = param("name")
          val defaultPackage = param("pack")
          val dependencies = param("dependencies")
          val flags = param("flags")
          handleIoException {
            new ProjectBuilder(name, defaultPackage, dependencies, flags, projectDirectory, scalaMajorVersion).build()
            Success("[]")
          }
        case ("POST", "/project/copy") =>
          val name = param("name")
          val source = new File(cbt_home / "examples" / name)
          val target = new File(projectDirectory.getAbsolutePath / name)
          handleIoException {
            copyTree(source, target)
            Success("[]")
          }
        case ("GET", "/dependency") =>
          val query = param("query")
          handleIoException(handleMavenBadResponse(searchDependency(query)))
        case ("GET", "/dependency/version") =>
          val group = param("group")
          val artifact = param("artifact")
          handleIoException(handleMavenBadResponse(searchDependencyVersion(group, artifact)))
        case ("GET", "/examples") =>
          handleIoException {
            val names = new File(cbt_home / "examples").listFiles().filter(_.isDirectory).sortBy(_.getName)
                .map('"' + _.getName + '"').mkString(",")
            Success(s"[$names]")
          }
        case ("GET", "/example/files") =>
          val name = param("name")
          handleIoException {
            val dir = new File(cbt_home / "examples" / name)
            if (dir.exists())
              Success(serializeTree(dir))
            else
              Failure(new IllegalArgumentException(s"Incorrect example name: $name"))
          }
        case ("GET", "/example/file") =>
          setContentType("text/plain")
          val path = param("path")
          handleIoException {
            val file = new File(path)
            Success {
              val content = Source.fromFile(file).mkString
              if (file.getName.endsWith(".md"))
                parseMd(content)
              else
                content
            }
          }
        case _ =>
          Failure(new MalformedURLException(s"Incorrect path: $path"))
      }
    }
    server.start()
    if(!java.awt.GraphicsEnvironment.isHeadless()) {
      java.awt.Desktop.getDesktop.browse(new java.net.URI(s"http://localhost:$uiPort/"))
    }

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

  private def serializeTree(file: File): String = {
    val data = if (file.isDirectory)
      s""","children":[${file.listFiles().sortBy(_.getName).map(serializeTree).mkString(",")}]"""
    else
      ""
    s"""{"name":"${file.getName}","path":"${file.getAbsolutePath}"$data}"""
  }

  private def parseMd(s: String) = {
    val parser = Parser.builder().build()
    val document = parser.parse(s)
    val renderer = HtmlRenderer.builder().build()
    renderer.render(document)
  }

  private def copyTree(sourceRoot: File, targetRoot: File) =
    listFilesRecursive(sourceRoot).sorted.foreach { source =>
      val path = source.getAbsolutePath.replace(sourceRoot.getAbsolutePath, targetRoot.getAbsolutePath)
      val file = new File(path)
      if (source.isDirectory)
        file.mkdirs()
      else
        Files.copy(source.toPath, file.toPath)
    }

  private def listFilesRecursive(f: File): Seq[File] =
    f +: (if (f.isDirectory) f.listFiles.flatMap(listFilesRecursive).toVector else Vector[File]())

}
