package cbt_build.proguard
import cbt._
import java.nio.file.Files._
import java.net._
import java.io._
import scala.xml._

class Build(val context: Context) extends Scalafmt{
  def description: String = "Type-safe scala wrapper to interfaces with ProGuard.main runner"
  def inceptionYear = 2017

  def generate = {
    lib.transformFiles( sourceFiles, replaceSections( _, replacements ) )
    compile
  }

  override def scalafmt = super.scalafmt.copy(
    config = super.scalafmt.lib.cbtRecommendedConfig,
    whiteSpaceInParenthesis = true
  )

  override def compile = {
    // currently suffers from non-deterministic formatting. Try a few times to reproduce commit state.
    val formatted = scalafmt.apply.map(_.string).mkString("\n")
    if( formatted.nonEmpty ) System.err.println( "Formatted:\n" ++ formatted ++ "\n---------------" )
    super.compile
  }

  def refcard = projectDirectory / "spec/refcard.html"

  /** downloads html proguard parameter specification */
  def updateSpec = {
    System.err.println(lib.blue("downloading ")+refcard)
    lib.download(
      new URL("https://www.guardsquare.com/en/proguard/manual/refcard"),
      refcard,
      None,
      replace = true
    )
    System.err.println("simplifying html")
    val tables = (
      loadSloppyHtml( refcard.readAsString ) \ "body" \\ "table"
    )
    val s = (
      "<html><body>\n" ++ tables.map( table =>
        " <table>\n" ++ (table \\ "tr").map( tr =>
          "  <tr>\n" ++ (tr \\ "td").map( td =>
            "   <td>" ++ td.text ++ "</td>\n"
          ).mkString ++ "  </tr>\n"
        ).mkString ++ " </table>\n"
      ).mkString ++ "</body></html>\n"
    )
    System.err.println("writing file")
    write( refcard.toPath, s.getBytes)
  }

  private def loadSloppyHtml(html: String): scala.xml.Elem = {
    object XmlNotDownloadingDTD extends scala.xml.factory.XMLLoader[scala.xml.Elem] {
      override def parser: javax.xml.parsers.SAXParser = {
        val f = javax.xml.parsers.SAXParserFactory.newInstance()
        f.setNamespaceAware(false)
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.newSAXParser()
      }
    }

    val p = new org.ccil.cowan.tagsoup.Parser
    val w = new StringWriter
    p.setContentHandler(new org.ccil.cowan.tagsoup.XMLWriter(w))
    p.parse(
      new org.xml.sax.InputSource(
        new ByteArrayInputStream( "<!DOCTYPE[^<]*>".r.replaceFirstIn( html, "" ).getBytes )
      )
    )
    XmlNotDownloadingDTD.loadString( w.toString )
  }

  /** generates Scala code from parameter specification html */
  def replacements = {
    val tables = XML.loadFile(refcard) \\ "table"
    def cellsToSeq( node: Node ) = (node \\ "tr").map(
      tr => (tr \\ "td").map( td => td.text ) match {
        case Seq( k, v ) => k -> v
      }
    )
    val options = cellsToSeq( tables(0) ).collect{
      case (k, v) if k.startsWith("-") => k.drop(1).split(" ").toList -> v
    }.map{
      case (k,description) =>
        val name = k(0)
        val tpe = k.drop(1).mkString(" ") match {
          case "" => "Boolean"
          case "n" => "Int"
          case "class_specification" | "version" | "optimization_filter" => "String"
          case "filename" | "directoryname" => "File"
          case "class_path" if name === "outjars" => "Seq[File]"
          case "class_path" => "Seq[File]"
          case "[filename]" => "Option[File]"
          case "[directory_filter]" | "[package_filter]" | "[package_name]"
               | "[attribute_filter]" | "[string]" | "[class_filter]" | "[file_filter]"
               => "Option[String]"
          case "[,modifier,...] class_specification" => "(Seq[KeepOptionModifier], String)"
        }
       (name, tpe, description.split("\n").mkString(" "))
    }//.sortBy(_._1)

    val docs = options.map{
      case (name, tpe, description) => s"  @param $name $description"
    }.mkString("\n")

    val params = options.map{
      case v@(_, "Boolean", _) => v -> Some("false")
      case (n, t, d) => (n, s"Option[$t]", d) -> Some("None")
    }.map{
      case ((name, tpe, description), default) => s"    $name: $tpe" ++ default.map(" = "++_).getOrElse("")
    }.mkString(",\n")

    val keepModifiers = cellsToSeq( tables(2) ).map{
      case (k, v) => s"""  /** $v */\n  object $k extends KeepOptionModifier("$k")"""
    }.mkString("\n")

    val args = options.map{
      case (name, _, description) => s"""argsFor($name).map("-$name" +: _)"""
    }.mkString("\n++ ")

    Seq(
      "keepModifiers" -> keepModifiers,
      "docs" -> docs,
      "args" -> args,
      "params" -> params
    )
  }
}
