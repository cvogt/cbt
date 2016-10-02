package cbt

import org.scalafmt.Error.Incomplete
import org.scalafmt.Formatted
import org.scalafmt.cli.StyleCache
import org.scalafmt.config.ScalafmtConfig
import java.io.File
import java.nio.file.Files._
import java.nio.file.{ FileSystems, Path, Paths }

/**
  * This plugin provides scalafmt support for cbt.
  *
  */
trait Scalafmt extends BaseBuild {
  /**
    * Reformat scala source code according to `scalafmtConfig` rules
    *
    * @return always returns `ExitCode.Success`
    */
  final def scalafmt: ExitCode = {
    Scalafmt.format(sourceFiles, scalafmtConfig)
    ExitCode.Success
  }

  /**
    * Scalafmt formatting config.
    *
    * Tries to get style in following order:
    * • project local .scalafmt.conf
    * • global ~/.scalafmt.conf
    * • default scalafmt config
    *
    * Override this task if you want to provide
    * scalafmt config programmatically on your own.
    */
  def scalafmtConfig: ScalafmtConfig =
    Scalafmt.getStyle(
      project = projectDirectory.toPath,
      home = Option(System.getProperty("user.home")) map (p => Paths.get(p))
    )
}

object Scalafmt {

  def getStyle(project: Path, home: Option[Path]): ScalafmtConfig = {
    val local = getConfigPath(project)
    val global = home flatMap getConfigPath
    val customStyle = for {
      configPath <- local.orElse(global)
      style <- StyleCache.getStyleForFile(configPath.toString)
    } yield style

    customStyle.getOrElse(ScalafmtConfig.default)
  }

  def format(files: Seq[File], style: ScalafmtConfig): Unit = {
    var reformattedCount: Int = 0
    scalaSourceFiles(files) foreach { path =>
      handleFormatted(path, style) { case (original, result) =>
        result match {
          case Formatted.Success(formatted) =>
            if (original != formatted) {
              write(path, formatted.getBytes)
              reformattedCount += 1
            }
          case Formatted.Failure(e: Incomplete) =>
            System.err.println(s"Couldn't complete file reformat: $path")
          case Formatted.Failure(e) =>
            System.err.println(s"Failed to format file: $path, cause: ${e}")
        }
      }
    }
    if (reformattedCount > 0) System.err.println(s"Formatted $reformattedCount Scala sources")
  }

  private val scalaFileMatcher = FileSystems.getDefault.getPathMatcher("glob:**.scala")

  private def scalaSourceFiles(files: Seq[File]): Seq[Path] = {
    files collect {
      case f if f.exists
        && scalaFileMatcher.matches(f.toPath) => f.toPath
    }
  }

  private def handleFormatted[T](path: Path, style: ScalafmtConfig)(handler: (String, Formatted) => T): T = {
    val original = new String(readAllBytes(path))
    val result = org.scalafmt.Scalafmt.format(original, style)
    handler(original, result)
  }

  private def getConfigPath(base: Path): Option[Path] = {
    val location = base.resolve(".scalafmt.conf").toFile
    Option(location.exists && location.isFile) collect {
      case true => location.toPath.toAbsolutePath
    }
  }

}
