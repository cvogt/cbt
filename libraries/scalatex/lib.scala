package cbt.plugins.scalatex.runtime

import scalatags.Text.TypedTag
import scalatags.Text.all._

import cbt.eval.Eval
import java.net._
import java.io._
import org.cvogt.scala._

object hl extends scalatex.site.Highlighter

object `package`{
  def comment(any: =>Any): Unit = ()
  def plain = span(fontFamily:="monospace")
  def highlight(code: String) =
    raw( """<pre class="highlight language-scala"><code>""" ++ code.stripIndent.trim ++ "</code></pre>" )
  def layout: SeqFrag[Frag] => String => String = { contents => path =>
    import scalatex._
    import scalatags.Text.tags2._
    import java.nio.file.{Paths, Files}
    import scalatags.stylesheet._
    val parts = path.split(File.separator)
    val base = ("../" * (parts.size - 1)).mkString
    val classes = parts.inits.toList.reverse.drop(1).map( parts => "path-" ++ parts.mkString("-") ).mkString(" ")
    html(
      head(
        link(rel:="stylesheet", `type`:="text/css", href := base+"style.css")
      ),
      body(
        `class` := classes,
        raw("\n"), contents, raw("\n")
      )
    ).render
  }
}

class repl(classLoader: ClassLoader, dependencyClasspath: java.io.File*) { repl =>
  private val eval = new Eval(){
    override lazy val impliedClassPath: List[String] = dependencyClasspath.map(_.toString).to
    override def classLoader = repl.classLoader
  }
  /**
    * repl session, inspired by tut.
    *
    * Example: code="1 + 1" returns
    * "scala> 1 + 1
    * res0: Int = 2"
    */
  def apply(code: String) = {
    // adapted from https://github.com/olafurpg/scalafmt/blob/a12141b5ce285a60886abab0d96044c617e20b51/readme/Readme.scala
    import scala.meta._
    import scala.util._
    val expressions = s"{$code}".parse[Stat].get.asInstanceOf[Term.Block].stats
    val output = Try(eval[Any](code)) match {
      case Success( value ) => "res0: " ++ value.getClass.getName ++ " = " ++ (value match {
        case v: String => "\"" ++ v ++ "\""
        case other => other.toString
      })
      case Failure( e ) => e.getMessage
    }

    val lineSeparator = String.format("%n")
    val prefix = code.lines.filterNot("\t ".contains).mkString(lineSeparator).commonLinePrefix
    val result = (
      expressions
        .map(prefix ++ _.toString)
        .map("scala> " ++ _.stripIndent.prefixLines("       ").trimLeft)
        .mkString("\n")
      ++
      "\n" ++ output.trim
    )

    hl.scala(result)
  }
}
