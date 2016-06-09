package cbt
import java.net.URL
case class License(name: String, url: URL)
object License{
  object Apache2 extends License(
    "Apache-2.0",
    new URL(s"http://www.apache.org/licenses/LICENSE-2.0")
  )
}
