package cbt

import org.scalafmt.{FormatResult, ScalafmtStyle}

import java.io.File
import java.nio.file.Files._
import java.nio.file.{FileSystems, Path}

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
    * Scalafmt formatting config
    */
  def scalafmtConfig: ScalafmtStyle = Scalafmt.defaultConfig
}

object Scalafmt {

  val defaultConfig = ScalafmtStyle.default

  def format(files: Seq[File], style: ScalafmtStyle): Unit = {
    var reformattedCount: Int = 0
    scalaSourceFiles(files) foreach { path =>
      handleFormatted(path, style) { case (original, result) =>
        result match {
          case FormatResult.Success(formatted) =>
            if (original != formatted) {
              write(path, formatted.getBytes)
              reformattedCount += 1
            }
          case FormatResult.Failure(e) =>
            System.err.println(s"Failed to format file: $path, cause: ${e}")
          case FormatResult.Incomplete(e) =>
            System.err.println(s"Couldn't complete file reformat: $path")
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

  private def handleFormatted[T](path: Path, style: ScalafmtStyle)(handler: (String, FormatResult) => T): T = {
    val original = new String(readAllBytes(path))
    val result = org.scalafmt.Scalafmt.format(original, style)
    handler(original, result)
  }

}
