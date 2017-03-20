package cbt

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files._

import scalariform.formatter.ScalaFormatter
import scalariform.formatter.preferences.FormattingPreferences
import scalariform.parser.ScalaParserException

trait Scalariform extends BaseBuild {
  def scalariform = Scalariform.apply(lib, sourceFiles.filter(_.string endsWith ".scala"), scalaVersion).config()
}

object Scalariform{
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

  case class apply( lib: Lib, files: Seq[File], scalaVersion: String ){
    case class config(
      preferences: FormattingPreferences = Scalariform.defaultPreferences
    ) extends (() => Seq[File]){
      def apply = {
        val (successes, errors) = lib.transformFilesOrError( files, in =>
          try{
            Right( ScalaFormatter.format( in, preferences, Some(scalaVersion) ) )
          } catch {
            case e: ScalaParserException => Left( e )
          }
        )
        if(errors.nonEmpty)
          throw new RuntimeException(
            "Scalariform failed to parse some files:\n" ++ errors.map{
              case (file, error) => file.string ++ ": " ++ error.getMessage
            }.mkString("\n"),
            errors.head._2
          )
        successes
      }
    }
  }
}
