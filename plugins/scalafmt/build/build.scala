import cbt._

class Build(val context: Context) extends Plugin {
  override def dependencies =
    super.dependencies ++
    Resolver( mavenCentral ).bind(
      ScalaDependency("com.geirsson", "scalafmt", "0.3.1")
    )
}
