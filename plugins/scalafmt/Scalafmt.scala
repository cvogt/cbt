package cbt

import org.scalafmt.Error.Incomplete
import org.scalafmt.Formatted
import org.scalafmt.cli.StyleCache
import org.scalafmt.config.ScalafmtConfig
import java.io.File
import java.nio.file.Files._
import java.nio.file._

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
  final def scalafmt: ExitCode = Scalafmt.format(sourceFiles, scalafmtConfig)

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

  def format(files: Seq[File], style: ScalafmtConfig): ExitCode = {
    val results = files.filter(_.string endsWith ".scala").map(_.toPath).map{ path =>
      val original = new String(readAllBytes(path))
      org.scalafmt.Scalafmt.format(original, style) match {
        case Formatted.Success(formatted) =>
          if (original != formatted) {
            val tmpPath = Paths.get(path.toString ++ ".scalafmt-tmp")
            write(tmpPath, formatted.getBytes)
            move(tmpPath, path, StandardCopyOption.REPLACE_EXISTING)
            Some(1)
          } else {
            Some(0)
          }
        case Formatted.Failure(e) =>
          System.err.println(s"Scalafmt failed for $path\nCause: $e\n")
          None
      }
    }
    if(results.forall(_.nonEmpty)){
      System.err.println(s"Formatted ${results.flatten.sum} Scala sources")
      ExitCode.Success
    } else ExitCode.Failure
  }

  private def getConfigPath(base: Path): Option[Path] = {
    Some( base.resolve(".scalafmt.conf").toFile )
      .collect{ case f if f.exists && f.isFile => f.toPath.toAbsolutePath }
  }
}
