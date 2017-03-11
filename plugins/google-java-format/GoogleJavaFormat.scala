package cbt

import java.io.File
import java.nio.file.Files._
import java.nio.file._

import com.google.googlejavaformat.java._

trait GoogleJavaFormat extends BaseBuild {
  def googleJavaFormat() = GoogleJavaFormat.apply( lib, sourceFiles.filter(_.string endsWith ".java") ).format
}

object GoogleJavaFormat{
  case class apply( lib: Lib, files: Seq[File] ){
    /** @param whiteSpaceInParenthesis more of a hack to make up for missing support in Scalafmt. Does not respect alignment and maxColumn. */
    def format = {
      val (successes, errors) = lib.transformFilesOrError( files, in =>
        try{
          Right( new Formatter().formatSource(in) )
        } catch {
          case e: FormatterException => Left( e )
        }
      )
      if(errors.nonEmpty)
        throw new RuntimeException(
          "Google Java Format failed to parse some files:\n" ++ errors.map{
            case (file, error) => file.string ++ ":" ++ error.toString
          }.mkString("\n"),
          errors.head._2
        )
      successes
    }
  }
}
