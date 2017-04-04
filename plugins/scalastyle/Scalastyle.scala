package cbt

import java.io.File
import java.util.Date
import scala.io.Codec
import com.typesafe.config.ConfigFactory

import org.scalastyle._

trait Scalastyle extends Plugin {
  def scalastyle = Scalastyle.apply( lib ).config( Scalastyle.defaultConfig, sourceFiles, getClass.getClassLoader )
}

object Scalastyle {
  def readConfigFromXml( file: File ) = ScalastyleConfiguration.readFromXml( file.string )
  def defaultConfig =
    ScalastyleConfiguration.readFromXml(
      Option( getClass.getClassLoader.getResource("scalastyle-config.xml") )
        .getOrElse( throw new Exception("scalastyle-config.xml not found in resources")
    ).getPath
  )

  case class apply( lib: Lib ){
    /** @param classLoader able to load the Checker classes */
    case class config(
      scalastyleConfig: ScalastyleConfiguration,
      files: Seq[File],
      classLoader: ClassLoader/*,
      xmlOutput: Option[File] = None,
      logLevel: Option[Level] = Some( InfoLevel )*/
    ){
      def apply: ExitCode = output( messages )


      def messages =
        new ScalastyleChecker( Some( classLoader ) )
          .checkFiles( scalastyleConfig, Directory.getFiles( None, files ) )

      def output( messages: List[Message[_]] ) = {
        val messageHelper = new MessageHelper( ConfigFactory.load( classLoader ) )

        def fileLineColumn( file: FileSpec, line: Option[Int], column: Option[Int] ) =
          file.name ~ line.map( ":" ~ _.toString ~ column.map( ":" ~ _.toString ).getOrElse( "" ) ).getOrElse("")

        val errors = messages.map(x => x:AnyRef).collect{
          case s@StyleError(
            file, cls, key, level, args, line, column, customMessage
          ) => (
            fileLineColumn( file, line, column ) ~ ": [" ~ cls.getSimpleName ~ "] "
              ~ Output.findMessage( messageHelper, key, args, customMessage )
          )
          case s@StyleException(file, cls, message, stacktrace, line, column) =>
            fileLineColumn( file, line, column ) ~ ": " ~ cls.map( "[" ~ _.getSimpleName ~ "] " ).getOrElse( "" ) ~ message ~ "\n" ~ stacktrace
        }

        if( errors.nonEmpty ){
          System.err.println(
            lib.red( "Scalastyle linting errors found\n" ) + errors.mkString("\n")
          )
          ExitCode.Failure
        } else {
          ExitCode.Success
        }

        /*
        xmlOutput.foreach(
          XmlOutput.save(config, _, encoding, messages)
        )

        if (!mc.quiet) println("Processed " + outputResult.files + " file(s)")
        if (!mc.quiet) println("Found " + outputResult.errors + " errors")
        if (!mc.quiet) println("Found " + outputResult.warnings + " warnings")
        if (!mc.quiet) println("Finished in " + (now - start) + " ms")

        outputResult.errors > 0 || (mc.warningsaserrors && outputResult.warnings > 0)
        */
      }
    }
  }
}