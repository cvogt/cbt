package prototype

import japgolly.scalajs.react.ReactDOM
import org.scalajs.dom

import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport

@JSExport("App")
object App extends JSApp {
  def main(): Unit = {
    val doc = dom.document
    ReactDOM.render(Pictures.PictureComponent(), doc.getElementById("main"))
  }
}
