import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

object Main extends App {
  println("fooo")
  val futureRes = Await.result(Future.successful(1), 5.seconds)
  List(1, 2, 4, 5, 6) match {
    case h :: _ ⇒ println("not empty list")
    case Nil ⇒ println("empty list")
  }
}
