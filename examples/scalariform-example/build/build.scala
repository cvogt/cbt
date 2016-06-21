import cbt._
import scalariform.formatter.preferences._

class Build(val context: Context) extends BaseBuild with Scalariform {
  override def compile = {
    scalariformFormat
    super.compile
  }

  override def scalariformPreferences =
    FormattingPreferences()
      .setPreference(SpacesAroundMultiImports, true)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(RewriteArrowSymbols, true)

  final def breakFormatting = {
    import java.nio.file._
    import scala.collection.JavaConverters._
    sourceFiles foreach { file =>
      try {
        val path = file.toPath
        val fileLines = Files.readAllLines(path).asScala
        val brokenLines = fileLines map (_.dropWhile(_ ==' '))
        Files.write(path, brokenLines.asJava)
      } catch {
        case e: Exception => System.err.print(s"Error happend when breaking formatting: ${e}")
      }
    }
    System.err.println("Done breaking formatting")
  }
}
