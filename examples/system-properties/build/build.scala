import cbt._

class Build(val context: Context) extends BaseBuild {

  def showProps: Unit = {
    val customProps: Map[String, String] = context.props
    val foo: Option[String] = customProps.get("foo")
    val bar: Option[String] = customProps.get("bar")

    println(s"=== Custom properties, foo: ${foo}, bar: ${bar}")

    val timezone: String = System.getProperty("user.timezone")
    val os: String = System.getProperty("os.name")

    println(s"=== System properties, timezone: ${timezone}, os: ${os}")
  }

}
