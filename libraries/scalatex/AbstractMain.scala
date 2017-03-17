package cbt.plugins.scalatex.runtime

import java.io.File
import java.nio.file.{ Files, Paths }
import scalatags.Text.all._
trait AbstractMain{
  def dependencyClasspath: Seq[File]
  def files: Seq[(String,SeqFrag[Frag])]
  def htmlTarget: File
  def render: SeqFrag[Frag] => String => String
  def main(args: Array[String]): Unit = {
    val changed = files.flatMap{
      case (name, contents) =>
        val path = htmlTarget.toPath.resolve(name + ".html")
        import Files._
        createDirectories(path.getParent)
        val newContents = render(contents)(name)
        if( !exists(path) || new String( Files.readAllBytes( path ) ) != newContents ){
          write( path, newContents.getBytes )
          Some( path )
        } else None
    }
    if( args.lift(0).map(_=="open").getOrElse(false) )
      changed.headOption.foreach{ file =>
        System.out.println(file)
        val old = Option(System.getProperty("apple.awt.UIElement"))
        System.setProperty("apple.awt.UIElement", "true")
        java.awt.Desktop.getDesktop().open(file.toFile)
        old.foreach(System.setProperty("apple.awt.UIElement", _))
      }
    else System.out.println(changed.mkString("\\n"))
  }
  val repl = new cbt.plugins.scalatex.runtime.repl( this.getClass.getClassLoader, dependencyClasspath: _* )
}
