
object Main extends App {
  val foo: Option[String] = Option(System.getProperty("foo"))
  val bar: Option[String] = Option(System.getProperty("bar"))

  println(s"=== Custom properties, foo: ${foo}, bar: ${bar}")

  val timezone: String = System.getProperty("user.timezone")
  val os: String = System.getProperty("os.name")

  println(s"=== System properties, timezone: ${timezone}, os: ${os}")
}
