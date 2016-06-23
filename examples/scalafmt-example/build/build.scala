import cbt._
import org.scalafmt.ScalafmtStyle

class Build(val context: Context) extends BuildBuild with Scalafmt {
  override def compile = {
    scalafmt
    super.compile
  }

  override def scalafmtConfig: ScalafmtStyle = ScalafmtStyle.defaultWithAlign

  def breakFormatting = {
    import java.nio.file._
    import java.nio.charset.Charset
    import scala.collection.JavaConverters._
    val utf8 = Charset.forName("UTF-8")
    sourceFiles foreach { file =>
      try {
        val path = file.toPath
        val fileLines = Files.readAllLines(path, utf8).asScala
        val brokenLines = fileLines map (_.dropWhile(_ ==' '))
        Files.write(path, brokenLines.asJava, utf8)
      } catch {
        case e: Exception => System.err.print(s"Error happend when breaking formatting: ${e}")
      }
    }
    System.err.println("Done breaking formatting")
  }
}
