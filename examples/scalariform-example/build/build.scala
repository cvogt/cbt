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
    import java.nio.charset.Charset
    import scala.collection.JavaConverters._
    val utf8 = Charset.forName("UTF-8")
    sourceFiles foreach { file =>
      val path = file.toPath
      val fileLines = Files.readAllLines(path, utf8).asScala
      val brokenLines = fileLines map (_.dropWhile(_ == ' '))
      Files.write(path, brokenLines.asJava, utf8)
    }
    System.err.println("Done breaking formatting")
  }
}
