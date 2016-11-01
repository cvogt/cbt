import cbt._

class Build(val context: Context) extends BaseBuild with Scalafmt {
  override def compile = {
    scalafmt
    super.compile
  }

  def breakFormatting = {
    import java.nio.file._
    import java.nio.charset.Charset
    import scala.collection.JavaConverters._
    val utf8 = Charset.forName("UTF-8")
    sourceFiles foreach { file =>
      val path = file.toPath
      val fileLines = Files.readAllLines(path, utf8).asScala
      val brokenLines = fileLines map (l =>
        l.dropWhile(_ == ' ')
          .replaceAll("⇒", "=>")
          .replaceAll("→", "->")
        )
      Files.write(path, brokenLines.asJava, utf8)
    }
    System.err.println("Done breaking formatting")
  }
}
