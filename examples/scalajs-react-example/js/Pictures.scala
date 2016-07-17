package prototype

import japgolly.scalajs.react.{ReactComponentB, BackendScope, Callback}
import org.scalajs.dom

import scala.scalajs._
import japgolly.scalajs.react.vdom.all._

import scala.scalajs.js.JSON

object Pictures {

  case class State(pictures: List[Picture], favourites: List[Picture])

  type PicClick = (String, Boolean) => Callback

  class Backend($: BackendScope[Unit, State]) {

    def onPicClick(id: String, favorite: Boolean) =
      $.state flatMap { s =>
        if (favorite) {
          val newPics = s.pictures.map(p => if (p.id == id) p.copy(favorite = false) else p)
          val newFavs = s.favourites.filter(p => p.id != id)
          $.modState(_ => State(newPics, newFavs))
        } else {
          var newPic: Picture = null
          val newPics = s.pictures.map(p => if (p.id == id) {
            newPic = p.copy(favorite = true); newPic
          } else p)
          val newFavs = s.favourites.+:(newPic)
          $.modState(_ => State(newPics, newFavs))
        }
      }

    def render(s: State) =
      div(
        h1("Popular Pixabay Pics"),
        pictureList((s.pictures, onPicClick)),
        h1("Your favorites"),
        favoriteList((s.favourites, onPicClick)))
  }

  val picture = ReactComponentB[(Picture, PicClick)]("picture")
    .render_P { case (p, b) =>
      div(if (p.favorite) cls := "picture favorite" else cls := "picture", onClick --> b(p.id, p.favorite))(
        img(src := p.src, title := p.title)
      )
    }
    .build

  val pictureList = ReactComponentB[(List[Picture], PicClick)]("pictureList")
    .render_P { case (list, b) =>
      div(`class` := "pictures")(
        if (list.isEmpty) span("Loading Pics..")
        else {
          list.map(p => picture.withKey(p.id)((p, b)))
        }
      )
    }
    .build

  val favoriteList = ReactComponentB[(List[Picture], PicClick)]("favoriteList")
    .render_P { case (list, b) =>
      div(`class` := "favorites")(
        if (list.isEmpty) span("Click an image to mark as  favorite")
        else {
          list.map(p => picture.withKey(p.id)((p, b)))
        }
      )
    }
    .build

  val PictureComponent = ReactComponentB[Unit]("PictureComponent")
    .initialState(State(Nil, Nil))
    .renderBackend[Backend]
    .componentDidMount(scope => Callback {
      import scalajs.js.Dynamic.{global => g}
      def isDefined(g: js.Dynamic): Boolean =
        g.asInstanceOf[js.UndefOr[AnyRef]].isDefined
      val url = "http://localhost:3000/data"
      val xhr = new dom.XMLHttpRequest()
      xhr.open("GET", url)
      xhr.onload = { (e: dom.Event) =>
        if (xhr.status == 200) {
          val result =  JSON.parse(xhr.responseText)
          if (isDefined(result) && isDefined(result.hits)) {
            val hits = result.hits.asInstanceOf[js.Array[js.Dynamic]]
            val pics = hits.toList.map(item => Picture(
              item.id.toString,
              item.pageURL.toString,
              item.previewURL.toString,
              if (item.tags != null) item.tags.asInstanceOf[js.Array[String]].mkString(",") else ""))
              //if (item.caption != null) item.caption.text.toString else ""))
            scope.modState(_ => State(pics, Nil)).runNow()
          }
        }
      }
      xhr.send()
    })
    .buildU

}
