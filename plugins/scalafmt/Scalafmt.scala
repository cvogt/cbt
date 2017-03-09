package cbt

import org.scalafmt.Error.Incomplete
import org.scalafmt.Formatted
import org.scalafmt.cli.StyleCache
import org.scalafmt.config.ScalafmtConfig
import java.io.File
import java.nio.file.Files._
import java.nio.file._

/** This plugin provides scalafmt support for cbt. */
trait Scalafmt extends BaseBuild {
  /** Reformat scala source code according to `scalafmtConfig` rules */
  def scalafmt = {
    val scalafmtLib = new ScalafmtLib(lib)
    scalafmtLib.format( sourceFiles ).config(
      scalafmtLib.loadConfig(
        projectDirectory.toPath
      ) getOrElse ScalafmtConfig.default
    )
  }
}

class ScalafmtLib(lib: Lib){ scalafmtLib =>
  def userHome = Option( System.getProperty("user.home") ).map(Paths.get(_))

  /** Tries to load config from .scalafmt.conf in given directory or fallback directory */
  def loadConfig( directory: Path, fallback: Option[Path] = userHome ): Option[ScalafmtConfig] = {
    def findIn( base: Path ): Option[Path] = {
      Some( base.resolve(".scalafmt.conf").toAbsolutePath ).filter(isRegularFile(_))
    }

    findIn( directory ).orElse( fallback flatMap findIn )
      .flatMap ( file => StyleCache.getStyleForFile(file.toString) )
  }

  case class format(files: Seq[File]){
    /**
      * @param whiteSpaceInParenthesis more of a hack to make up for missing support in Scalafmt. Does not respect alignment and maxColumn.
      */
    case class config(
      config: ScalafmtConfig,
      whiteSpaceInParenthesis: Boolean = false
    ) extends (() => Seq[File]){
      def lib = scalafmtLib
      def apply = {
        val (successes, errors) = scalafmtLib.lib.transformFilesOrError(
          files,
          org.scalafmt.Scalafmt.format(_, config) match {
            case Formatted.Success(formatted) => Right(
              if( whiteSpaceInParenthesis ){
                scalafmtLib.whiteSpaceInParenthesis(formatted)
              } else formatted
            )
            case Formatted.Failure( e ) => Left( e )
          }
        )
        if(errors.nonEmpty)
          throw new RuntimeException(
            "Scalafmt failed to format some files:\n" ++ errors.map{
              case (file, error) => file.string ++ ": " ++ error.getMessage
            }.mkString("\n"),
            errors.head._2
          )
        successes
      }
    }
  }

  private def whiteSpaceInParenthesis: String => String =
    Seq(
      "(\\(+)([^\\s\\)])".r.replaceAllIn(_:String, m => m.group(1).mkString(" ") ++ " " ++ m.group(2) ),
      "([^\\s\\(])(\\)+)".r.replaceAllIn(_:String, m => m.group(1) ++ " " ++ m.group(2).mkString(" ") )
    ).reduce(_ andThen _)

  def cbtRecommendedConfig = {
    import org.scalafmt.config._
    val c = ScalafmtConfig.defaultWithAlign
    c.copy(
      maxColumn = 110,
      continuationIndent = c.continuationIndent.copy(
        defnSite = 2
      ),
      align = c.align.copy(
        tokens = AlignToken.default ++ Set(
          // formatting for these alignment tokens seems to be deterministic right now
          // Maybe because of scala meta. Killing the class loader between runs seems not to help.
          // Starting a new jvm each time does.
          AlignToken( "=>", ".*" ),
          AlignToken( ":", ".*" ),
          AlignToken( "=", ".*" )
        ) + AlignToken.caseArrow,
        arrowEnumeratorGenerator = true,
        mixedOwners = true
      ),
      binPack = c.binPack.copy(
        parentConstructors = true
      ),
      spaces = c.spaces.copy(
        inImportCurlyBraces = true
      ),
      lineEndings = LineEndings.unix,
      newlines = c.newlines.copy(
        penalizeSingleSelectMultiArgList = false
      ),
      runner = c.runner.copy(
        optimizer = c.runner.optimizer.copy(
          forceConfigStyleOnOffset = -1
        )
      )
    )
  }
}
