package cbt

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files._

import scalariform.formatter.ScalaFormatter
import scalariform.formatter.preferences.FormattingPreferences
import scalariform.parser.ScalaParserException

trait Scalariform extends BaseBuild {
  final def scalariformFormat: ExitCode = {
    Scalariform.format(sourceFiles, scalariformPreferences, scalaVersion)
    ExitCode.Success
  }

  def scalariformPreferences: FormattingPreferences = Scalariform.defaultPreferences
}

object Scalariform {

  val defaultPreferences: FormattingPreferences = {
    import scalariform.formatter.preferences._
    FormattingPreferences()
      .setPreference(AlignParameters, true)
      .setPreference(AlignArguments, true)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(MultilineScaladocCommentsStartOnFirstLine, true)
      .setPreference(SpaceInsideParentheses, true)
      .setPreference(SpacesWithinPatternBinders, true)
      .setPreference(SpacesAroundMultiImports, true)
      .setPreference(DoubleIndentClassDeclaration, false)
  }

  private val scalaFileMatcher = FileSystems.getDefault.getPathMatcher("glob:**.scala")

  def format(files: Seq[File], preferences: FormattingPreferences, scalaVersion: String): Unit = {
    var reformattedCount: Int = 0
    for (file <- files if file.exists) {
      val path = file.toPath
      if(scalaFileMatcher.matches(path)) {
        try {
          val sourceCode = new String(readAllBytes(path))
          val formatted = ScalaFormatter.format(
            sourceCode,
            preferences,
            Some(scalaVersion)
          )
          if (sourceCode != formatted) {
            write(path, formatted.getBytes)
            reformattedCount += 1
          }
        } catch {
          case e: ScalaParserException => System.err.println(s"Scalariform parser error: ${e.getMessage} when formatting: $file")
        }
      }
    }
    if (reformattedCount > 0) System.err.println(s"Formatted $reformattedCount Scala sources")
  }

}
