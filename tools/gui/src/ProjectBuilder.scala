import java.io.File
import java.nio.file._

import Main.StringExtensionMethods

import scala.io.Source

class ProjectBuilder(
    name: String,
    defaultPackage: String,
    dependencyString: String,
    flagsString: String,
    location: File,
    scalaMajorVersion: String
) {

  private val newLine = System.lineSeparator()
  private val blankLine = newLine + newLine
  private val templateDir = System.getenv("CBT_HOME") / "tools" / "gui" / "resources" / "template-project"
  private val projectPath = location.getPath / name
  private val buildDir = projectPath / "build"
  private val mainDir = projectPath / "src" / "main" / "scala" / defaultPackage.split("\\.").reduce(_ / _)
  private val testDir = projectPath / "src" / "test" / "scala" / defaultPackage.split("\\.").reduce(_ / _)

  private val dependencies =
    if (dependencyString.isEmpty)
      ""
    else {
      def parseDependencies(input: String): Seq[Dependency] = {
        input.split(" ").map { x =>
          val xs = x.split("/")
          Dependency(xs(0), xs(1), xs(2))
        }
      }

      val list = parseDependencies(dependencyString)
      val str = list.map("      " ++ _.serialized).mkString(s",$newLine")
      s"""  override def dependencies = {
         |    super.dependencies ++ Resolver(mavenCentral).bind(
         |$str
         |    )
         |  }""".stripMargin
    }

  private val flags = {
    val map = flagsString.split(" ").map { x =>
      val Array(a, b) = x.split("/")
      (a, b)
    }.toMap
    Flags(map("readme") == "true", map("dotty") == "true", map("uberJar") == "true", map("wartremover") == "true")
  }

  private val plugins = {
    var content = ""
    if (flags.dotty)
      content += " with Dotty"
    if (flags.uberJar)
      content += " with UberJar"
    if (flags.wartremover)
      content += " with WartRemover"
    content
  }

  private val buildBuildDependencies = {
    var content = ""
    if (flags.wartremover)
      content += " :+ plugins.wartremover"
    content
  }

  def build(): Unit = {
    new File(buildDir).mkdirs()
    new File(mainDir).mkdirs()
    new File(testDir).mkdirs()
    addBuild()
    if (buildBuildDependencies.nonEmpty)
      addBuildBuild()
    addMain()
    addReadme(flags)
  }

  private def writeTemplate(templatePath: String, targetPath: String, replacements: (String, String, String)*) = {
    var content = Source.fromFile(templatePath).mkString
    for (replacement <- replacements)
      if (replacement._1.nonEmpty)
        content = content.replace(replacement._3, replacement._2)
      else
        content = content.replace(replacement._3, "")
    write(new File(targetPath), content, StandardOpenOption.CREATE_NEW)
  }

  private def addBuild() =
    writeTemplate(
      templateDir / "build" / "build.scala",
      buildDir / "build.scala",
      (name, s"""  override def projectName = "$name"$blankLine""", "##projectName##"),
      (dependencyString, dependencies + blankLine, "##dependencies##"),
      (plugins, plugins, "##with##")
    )

  private def addBuildBuild() =
    writeTemplate(
      templateDir / "build" / "build" / "build.scala",
      buildDir / "build" / "build.scala",
      (buildBuildDependencies, buildBuildDependencies, "##plus##")
    )

  private def addMain() =
    writeTemplate(
      templateDir / "src" / "main" / "scala" / "Main.scala",
      mainDir / "Main.scala",
      (defaultPackage, s"package $defaultPackage$blankLine", "##package##")
    )

  private def addReadme(flags: Flags) = {
    if (flags.readme) {
      val content =
        if (name.nonEmpty)
          s"# $name$blankLine"
        else
          ""
      write(new File(projectPath / "readme.md"), content, StandardOpenOption.CREATE_NEW)
    }
  }

  private case class Dependency(group: String, artifact: String, version: String) {
    val nameVersion = """^(.+)_(\d+\.\d+)$""".r
    val (name, scalaVersion) = artifact match {
      case nameVersion(n, v) => (n, Some(v))
      case str => (str, None)
    }

    def serialized =
      if (scalaVersion.contains(scalaMajorVersion))
        s"""ScalaDependency("$group", "$name", "$version")"""
      else
        s"""MavenDependency("$group", "$artifact", "$version")"""
  }

  private def write(file: File, content: String, option: OpenOption) = {
    file.getParentFile.mkdirs()
    Files.write(file.toPath, content.getBytes, option)
  }

  private case class Flags(readme: Boolean, dotty: Boolean, uberJar: Boolean, wartremover: Boolean)

}
