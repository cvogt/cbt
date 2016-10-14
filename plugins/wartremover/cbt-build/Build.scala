import cbt._

class Build(val context: Context) extends Plugin {
  override def dependencies =
    super.dependencies ++
    Resolver( mavenCentral ).bind(
      ScalaDependency("org.wartremover", "wartremover", "1.1.1")
    )
}
