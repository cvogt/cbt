package prototype

import japgolly.scalajs.react.ReactDOM
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.JSApp
import scala.scalajs.js.annotation.JSExport
import scalajs.js.Dynamic.{global => g}

/**
 * Created by katrin on 2016-04-10.
 */
@JSExport("App")
object App extends JSApp {

  def main(): Unit = {

    val doc = dom.document
    ReactDOM.render(Pictures.PictureComponent(), doc.getElementById("main"))
  }
}
