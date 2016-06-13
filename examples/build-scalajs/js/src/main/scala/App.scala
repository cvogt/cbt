package prototype

import japgolly.scalajs.react.ReactDOM
import org.scalajs.dom
import scala.scalajs.js.JSON
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

    val trace1 = """{
                 |  "x": [1, 2, 3, 4],
                 |  "y": [10, 15, 13, 17],
                 |  "type": "scatter"
                 |}  """.stripMargin

    val trace2 = """{
                   | "x": [1, 2, 3, 4],
                   |  "y": [16, 5, 11, 9],
                   |  "type": "scatter"
                   |}  """.stripMargin

    val data = js.Array(trace1, trace2).map(trace => JSON.parse(trace))

    val doc = dom.document
    Plotly.newPlot("plotly", data)
    ReactDOM.render(Pictures.PictureComponent(), doc.getElementById("main"))
  }
}
