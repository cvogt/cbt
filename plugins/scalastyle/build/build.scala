import cbt._

class Build(val context: Context) extends Plugin {
  private val ScalaStyleVersion = "0.8.0"

  override def dependencies = (
    super.dependencies ++ 
    Resolver( mavenCentral ).bind(
      ScalaDependency( "org.scalastyle", "scalastyle", ScalaStyleVersion )
    )
  )
}
