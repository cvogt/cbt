import cbt._

class Build(val context: Context) extends Plugin {
  private val ScalafmtVersion = "0.6.2"

  override def dependencies =
    super.dependencies ++
    Resolver( mavenCentral ).bind(
      ScalaDependency("com.geirsson", "scalafmt-cli", ScalafmtVersion)
    )
}
